# OrderService#createOrder 流程分析

本文档基于对 [OrderService.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java) 及其上下游调用代码的静态阅读，按 web → service → dao 三层梳理用户下单的完整逻辑，并指出其中的潜在风险。分析过程中未修改任何源代码。

## 调用链路

### Web 层

1. 入口为 [ApiServlet.handleCreateOrder](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/servlet/ApiServlet.java#L208-L218)（路由 `POST /api/orders`，由 [ApiServlet.handle](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/servlet/ApiServlet.java#L101-L104) 派发）。
2. [AuthFilter.doFilter](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/filter/AuthFilter.java#L26-L47) 拦截 `/api/*`，未登录请求会被直接以 `401 UNAUTHORIZED` 拒绝；登录态由 `SessionUtil` 维护。
3. `handleCreateOrder` 通过 `requireCurrentUserId(req)` 取出当前会话用户 ID，使用 `JsonUtil.parseBody(req)` 解析请求体，并通过 `getRequiredString` 取出 `contactName / contactPhone / deliveryAddress`，最后调用 `orderService.createOrder(userId, ...)` 并把返回的 `OrderDTO` 通过 `JsonUtil.writeSuccess` 序列化返回。

### Service 层

[OrderService.createOrder](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L30-L77) 是下单的核心编排：

1. 通过 [ValidationUtil.requireLength](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/util/ValidationUtil.java#L18-L26) 校验 `contactName(1-50)`、`contactPhone(1-30)`、`deliveryAddress(1-255)` 的非空与长度。
2. 通过 [DataSourceProvider.getConnection()](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L40) 申请连接并 `setAutoCommit(false)` 开启事务。
3. 调用 `cartDao.listByUserForUpdate(conn, userId)` 加 `FOR UPDATE` 行锁读取购物车。
4. 业务校验：购物车空 → `EMPTY_CART`；数量 ≤ 0 → `INVALID_CART`；命中下架菜品 → `MENU_ITEM_UNAVAILABLE`。
5. `cartDao.sumTotal(cartItems)` 累加金额 → `orderDao.createOrder` 写入 `orders` 主表 → `orderDao.insertOrderItems` 批量写入 `order_items` 快照 → `cartDao.clearByUser(conn, userId)` 清空购物车 → `conn.commit()` 提交。
6. 提交成功后调用 `orderDao.listByUser(userId, true)` 重新查询并 `stream().filter(id == orderId).findFirst()` 取出新建订单返回。
7. 异常处理：捕获 `ApiException` 时 `rollback` 后原样抛出；捕获 `Exception` 时 `rollback` 并改写为 `500 INTERNAL_ERROR`；`finally` 中将 `autoCommit` 还原为 `true`；最外层捕获 `SQLException`，统一抛出 `500 INTERNAL_ERROR "数据库事务异常"`。

### Dao 层

- [CartDao.listByUserForUpdate](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L23-L25) → 复用 [CartDao.listByUser(conn,userId,forUpdate=true)](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L27-L56)，SQL 为 `SELECT ... FROM cart_items c JOIN menu_items m ... WHERE c.user_id = ? ORDER BY c.id DESC FOR UPDATE`，把菜品的 `name / price / is_available` 一并读入 `CartItemDTO`。
- [CartDao.sumTotal](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L115-L119) → 在 Java 内对 `lineTotal` 做 `BigDecimal::add` 求和。
- [OrderDao.createOrder](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L23-L47) → `INSERT INTO orders (..., status='PLACED', total_amount, ...)` 并通过 `RETURN_GENERATED_KEYS` 取回订单主键。
- [OrderDao.insertOrderItems](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L49-L64) → 使用 `addBatch / executeBatch` 写入 `order_items`，保留 `item_name_snapshot / unit_price_snapshot` 等快照字段。
- [CartDao.clearByUser(conn,userId)](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L105-L113) → `DELETE FROM cart_items WHERE user_id = ?`，复用外层事务连接。
- [OrderDao.listByUser](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L66-L81) → 自行 `DataSourceProvider.getConnection()` 取新连接查询用户所有订单，并通过 `mapOrders → loadItemsByOrderIds` 批量回填订单明细。

## 潜在风险

### 1. `menu_items` 未加锁导致价格 / 上下架状态的 TOCTOU

下单事务中虽然对 `cart_items` 加了 `FOR UPDATE`（见 [CartDao.listByUser](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L27-L56)），但 SQL 通过 `JOIN menu_items m` 把 `m.price`、`m.is_available`、`m.price * c.quantity` 一并读出，却没有对 `menu_items` 任何形式的锁。如果管理员在事务窗口内修改菜品价格或将菜品下架（参考 `handleUpdateMenuItem`），就会出现：

- 已读到旧价的事务把旧 `price` 写入 `order_items.unit_price_snapshot`，而最新的菜单价格已经发生变化，对账与展示会与实际订单金额不一致；
- `is_available` 检查（[OrderService.java#L51-L53](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L51-L53)）只在事务开始的快照中生效，紧随其后的 `INSERT order_items` 之间没有再次校验，仍可能下出"已下架商品"的订单。

该问题属于典型的 TOCTOU（Time-Of-Check vs Time-Of-Use）。建议：要么对涉及的 `menu_items` 行也使用 `FOR UPDATE`/`LOCK IN SHARE MODE`，要么在 `INSERT order_items` 之前再次校验价格与上下架状态。

### 2. 缺少库存校验，存在超卖隐患

`createOrder` 全程只校验 `quantity > 0` 与 `is_available`，并未读取或扣减任何库存字段（[OrderService.java#L43-L60](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L43-L60) 与 [OrderDao.createOrder](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L23-L47)）。这意味着无论库存是否为 0、并发下单是否会突破库存，订单都能写入成功。一旦运营场景需要限量供应（如"今日限量 50 份"），当前实现会直接超卖；即便业务上目前默认无限量供应，也应该至少在 `menu_items` 中显式声明，并在服务层加注释，否则后续接入库存极易出现并发问题。

### 3. 提交后再查订单的"全量查询 + 内存过滤"反模式

`createOrder` 在 `conn.commit()` 之后通过 `orderDao.listByUser(userId, true).stream().filter(o -> o.id() == orderId).findFirst()` 来取回新订单（[OrderService.java#L61-L64](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L61-L64)），存在以下问题：

- 该调用会拉取该用户所有历史订单及其全部 `order_items`，随用户订单数量线性增长，造成不必要的 IO 和带宽占用，重度用户下单延迟会随历史订单累积而恶化；
- 业务语义上明明只关心刚生成的 `orderId`，但实际却扫了所有订单，属于典型的"先 SELECT 全量再内存 filter"反模式；
- `listByUser` 自行通过 `DataSourceProvider.getConnection()` 申请独立连接（[OrderDao.java#L72](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L72)），与事务内连接不是同一条；如果连接池容量紧张，下单链路会"持有事务连接 → 再申请第二条连接"，叠加事务期间的写操作会增加连接耗尽与死锁风险。

建议新增 `OrderDao.findById(orderId, includeItems)` 这类点查接口，提交事务后直接按主键查询返回，避免全表扫与连接二次申请。

### 4. （附加）异常路径的鲁棒性问题

- 进入 `catch (SQLException ex)` 的最外层时仅返回 `数据库事务异常`，但触发它的可能是 `conn.commit()`、`conn.rollback()` 或 `conn.setAutoCommit(true)` 失败；此时事务的真实落库状态对调用方不可见，业务侧难以判断是否需要补偿。
- `finally { conn.setAutoCommit(true); }` 在连接关闭前再次操作连接，若该调用本身抛 `SQLException` 会覆盖 `try` 中正常返回的语义；建议把 `setAutoCommit` 的恢复包一层 `try/catch` 仅记录日志，避免掩盖真实失败原因。
