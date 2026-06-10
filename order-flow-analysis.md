# 用户下单流程分析

## 调用链路

### 整体架构（Web → Service → DAO）

本系统采用经典的三层架构，下单流程从 HTTP 请求入口开始，经过业务逻辑层，最终完成数据库持久化。

### 1. Web 层（入口）

**类：** [ApiServlet.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/servlet/ApiServlet.java)

- 所有 HTTP 请求首先被 `ApiServlet#handle` 方法（第 65 行）统一路由分发
- POST `/api/orders` 请求匹配到 `handleCreateOrder` 方法（第 208-218 行）
- 流程步骤：
  1. 调用 `requireCurrentUserId(req)` 从 Session 中获取当前登录用户 ID，未登录则抛出 401 UNAUTHORIZED
  2. 使用 `JsonUtil.parseBody(req)` 解析 JSON 请求体
  3. 通过 `getRequiredString` 从 body 中提取三个必填字段：`contactName`、`contactPhone`、`deliveryAddress`
  4. 调用 `orderService.createOrder(userId, contactName, contactPhone, deliveryAddress)` 进入业务层
  5. 将返回的 `OrderDTO` 通过 `JsonUtil.writeSuccess` 序列化为 JSON 响应

### 2. Service 层（核心业务逻辑）

**类：** [OrderService.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java)

**方法：** `createOrder`（第 30-77 行），完整流程如下：

| 步骤 | 代码行 | 操作说明 |
|------|--------|----------|
| 1 | 36-38 | 参数校验：使用 `ValidationUtil.requireLength` 校验联系人（1-50字符）、电话（1-30字符）、地址（1-255字符）的长度 |
| 2 | 40 | 获取数据库连接 `DataSourceProvider.getConnection()`，开启 try-with-resources |
| 3 | 41 | `conn.setAutoCommit(false)` 关闭自动提交，开启手动事务控制 |
| 4 | 43 | `cartDao.listByUserForUpdate(conn, userId)` 用 SELECT ... FOR UPDATE 查询并锁定该用户购物车项 |
| 5 | 44-46 | 校验购物车非空，否则抛出 EMPTY_CART 异常 |
| 6 | 47-54 | 遍历购物车项：检查数量 > 0、菜品 is_available = true |
| 7 | 56 | `cartDao.sumTotal(cartItems)` 在内存中计算订单总金额 |
| 8 | 57 | `orderDao.createOrder(conn, ...)` 插入 orders 主表，状态设为 PLACED，返回生成的 orderId |
| 9 | 58 | `orderDao.insertOrderItems(conn, orderId, cartItems)` 批量插入 order_items 快照表（菜品名称、价格快照） |
| 10 | 59 | `cartDao.clearByUser(conn, userId)` 删除该用户所有购物车项 |
| 11 | 60 | `conn.commit()` 提交事务 |
| 12 | 61-64 | `orderDao.listByUser(userId, true)` 重新查询该用户所有订单（含订单项），在内存中过滤出刚创建的订单返回 |
| 13 | 65-67 | 捕获 ApiException：回滚事务后重抛 |
| 14 | 68-70 | 捕获其他 Exception：回滚事务，包装为 500 错误抛出 |
| 15 | 72 | finally 块：恢复 `conn.setAutoCommit(true)` |

### 3. DAO 层（数据访问）

涉及两个 DAO 类：

#### CartDao - [CartDao.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java)

| 方法 | 代码行 | SQL / 说明 |
|------|--------|------------|
| `listByUserForUpdate` | 23-25 | 调用内部 `listByUser(conn, userId, true)` |
| `listByUser (private)` | 27-56 | SELECT cart_items JOIN menu_items 并追加 `FOR UPDATE` 行锁 |
| `sumTotal` | 115-119 | 纯内存计算：对 List 中所有 CartItemDTO.lineTotal 做 BigDecimal 累加 |
| `clearByUser(Connection, long)` | 105-113 | `DELETE FROM cart_items WHERE user_id = ?` 删除用户全部购物车项（在事务连接中执行） |

#### OrderDao - [OrderDao.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java)

| 方法 | 代码行 | SQL / 说明 |
|------|--------|------------|
| `createOrder` | 23-47 | INSERT INTO orders，使用 Statement.RETURN_GENERATED_KEYS 获取自增主键 |
| `insertOrderItems` | 49-64 | 批量 INSERT INTO order_items（使用 addBatch/executeBatch），保存菜品名称、单价的历史快照 |
| `listByUser` | 66-81 | 新开数据库连接，JOIN users 查询该用户所有订单列表，调用 `mapOrders` 填充订单项 |
| `mapOrders` | 138-180 | 先收集所有 orderId，再调用 `loadItemsByOrderIds` 批量查询 order_items 合并到结果中 |
| `loadItemsByOrderIds` | 182-206 | SELECT FROM order_items WHERE order_id IN (...) 批量加载订单项 |

### 完整调用链总结

```
HTTP POST /api/orders
  └── ApiServlet#handleCreateOrder       (Web 层)
        ├── SessionUtil.currentUserId    (从 session 取 userId)
        ├── JsonUtil.parseBody           (解析 JSON)
        └── OrderService#createOrder     (Service 层 - 开启事务)
              ├── ValidationUtil.requireLength (参数校验)
              ├── DataSourceProvider.getConnection (获取连接)
              ├── CartDao#listByUserForUpdate (SELECT ... FOR UPDATE 锁定购物车)
              ├── CartDao#sumTotal       (内存计算总价)
              ├── OrderDao#createOrder   (INSERT orders 主表)
              ├── OrderDao#insertOrderItems (批量 INSERT order_items 快照)
              ├── CartDao#clearByUser    (DELETE 购物车)
              ├── Connection#commit      (提交事务)
              └── OrderDao#listByUser    (新开连接查询所有订单，过滤返回当前订单)
```

---

## 潜在风险

### 风险一：订单已提交成功但返回失败，导致用户端数据不一致

**位置：** [OrderService.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L60-L70) 第 60-70 行

**问题描述：**
代码第 60 行 `conn.commit()` 执行成功后，订单数据和购物车清空已经永久持久化到数据库。但第 61-64 行的 `orderDao.listByUser(userId, true)` 仍在内部 try 块中：
- 该方法会自己获取新的数据库连接，查询用户**所有**订单
- 如果此时数据库连接池耗尽、网络波动、或查询出现任何异常
- 异常会被第 68 行的 `catch (Exception ex)` 捕获
- 代码随后调用 `conn.rollback()`（但 commit 已完成，rollback 无效）
- 最终向客户端抛出 `ApiException(500, "INTERNAL_ERROR", "创建订单失败")`

**后果：**
用户看到"创建订单失败"的错误提示，重复下单，导致产生重复订单。用户购物车已清空，但前端误判为下单失败。

**影响程度：高** — 在数据库压力大或网络不稳定时偶发，直接影响用户体验和数据正确性。

---

### 风险二：并发添加购物车项会被意外删除，导致商品丢失

**位置：** [OrderService.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L43-L59) + [CartDao.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L105-L113)

**问题描述：**
`SELECT ... FOR UPDATE` 只会锁定事务开始时**已经存在**的 cart_items 行。并发场景下：

| 时间点 | 请求 A（下单） | 请求 B（加购） |
|--------|---------------|---------------|
| T1 | BEGIN; SELECT ... FOR UPDATE → 锁定 cart_item_1, cart_item_2 | |
| T2 | 计算总价、创建订单中... | INSERT INTO cart_items (menu_item_3) → **成功！新行未被锁定** |
| T3 | | COMMIT（加购完成，购物车有3项） |
| T4 | DELETE FROM cart_items WHERE user_id = ? → **删除了包括 cart_item_3 在内的所有购物车项** | |
| T5 | COMMIT（下单完成，订单里只有前2项） | |

**后果：**
用户在下单的同时新添加的商品（cart_item_3）既没有出现在订单里，又被从购物车中清除，用户不知情地丢失了已选商品。

**根本原因：**
`clearByUser` 使用 `DELETE FROM cart_items WHERE user_id = ?` 无条件全删，而不是只删除本次下单对应的 cart_item_id 列表。

**影响程度：中高** — 在用户操作较快或网络延迟时容易触发，造成用户购物车数据丢失。

---

### 其他发现的风险点（附加）

1. **联系电话无格式校验：** 只校验长度 1-30，可输入任意字符（包括特殊符号、SQL 片段等），虽有 PreparedStatement 防止 SQL 注入，但业务层面缺少手机号/电话格式验证。见 [ValidationUtil.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/util/ValidationUtil.java#L18-L26)。

2. **性能问题：返回单订单却查询全部历史：** [OrderService.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L61-L64) 第 61 行调用 `listByUser` 查询该用户所有订单及所有订单项，再在内存中 filter 出一个订单。随着用户订单量增长，此操作会越来越慢（O(N) 数据传输 + 内存过滤）。正确做法是提供一个按 orderId 单独查询的 DAO 方法。

3. **FOR UPDATE 不锁定 menu_items 行：** `SELECT ... FOR UPDATE` 因 JOIN 条件只锁定 cart_items，未锁定 menu_items。若下单过程中管理员修改价格，订单快照使用查询时的价格虽可接受，但未对菜品存在性/状态做二次行级锁定。
