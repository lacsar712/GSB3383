# 用户下单流程分析

## 调用链路

### Web 层（入口）

1. 客户端发送 `POST /api/orders`，请求体包含 `contactName`、`contactPhone`、`deliveryAddress`
2. `AuthFilter` 拦截请求，调用 `SessionUtil.isLoggedIn(req)` 校验登录状态，未登录返回 401
3. `ApiServlet#handleCreateOrder` 从 Session 中提取 `userId`，解析 JSON 请求体获取收货信息
4. 调用 `orderService.createOrder(userId, contactName, contactPhone, deliveryAddress)`

**关键代码位置**：[ApiServlet.java#L208-L218](src/main/java/com/example/ordering/web/servlet/ApiServlet.java#L208-L218)

### Service 层（核心逻辑）

`OrderService#createOrder` 执行以下步骤：

1. **参数校验**：通过 `ValidationUtil.requireLength` 校验联系人（1-50 字符）、联系电话（1-30 字符）、配送地址（1-255 字符）
2. **开启事务**：从 `DataSourceProvider` 获取连接，设置 `autoCommit=false`
3. **锁定并读取购物车**：调用 `cartDao.listByUserForUpdate(conn, userId)`，使用 `SELECT ... FOR UPDATE` 锁定购物车行
4. **业务校验**：
   - 购物车为空则抛出 `EMPTY_CART`
   - 购物车项数量 ≤ 0 则抛出 `INVALID_CART`
   - 购物车中存在已下架菜品则抛出 `MENU_ITEM_UNAVAILABLE`
5. **计算总价**：调用 `cartDao.sumTotal(cartItems)` 在 Java 侧对 `lineTotal` 求和
6. **创建订单**：调用 `orderDao.createOrder(conn, userId, total, ...)` 插入 `orders` 表，状态为 `PLACED`
7. **创建订单明细**：调用 `orderDao.insertOrderItems(conn, orderId, cartItems)` 批量插入 `order_items` 表
8. **清空购物车**：调用 `cartDao.clearByUser(conn, userId)` 删除该用户所有购物车项
9. **提交事务**：`conn.commit()`
10. **回查订单**：调用 `orderDao.listByUser(userId, true)`（**新连接**）查询所有订单，在 Java 侧按 `orderId` 过滤返回

异常处理：`ApiException` 和其他 `Exception` 均会触发 `conn.rollback()`，`SQLException` 在外层捕获并包装为 500 错误。

**关键代码位置**：[OrderService.java#L30-L77](src/main/java/com/example/ordering/service/OrderService.java#L30-L77)

### DAO 层（数据访问）

| 方法 | SQL 操作 | 连接来源 |
|------|---------|---------|
| `CartDao#listByUserForUpdate` | `SELECT ... FROM cart_items c JOIN menu_items m ... WHERE c.user_id = ? FOR UPDATE` | 事务连接 |
| `CartDao#sumTotal` | 纯 Java 计算，无 SQL | 无 |
| `OrderDao#createOrder` | `INSERT INTO orders (user_id, status, total_amount, ...) VALUES (?, 'PLACED', ?, ...)` | 事务连接 |
| `OrderDao#insertOrderItems` | `INSERT INTO order_items (order_id, menu_item_id, ...) VALUES (?, ?, ...)` 批量插入 | 事务连接 |
| `CartDao#clearByUser` | `DELETE FROM cart_items WHERE user_id = ?` | 事务连接 |
| `OrderDao#listByUser` | `SELECT ... FROM orders o JOIN users u ... WHERE o.user_id = ?` | **新连接** |

**关键代码位置**：
- [CartDao.java#L23-L56](src/main/java/com/example/ordering/dao/CartDao.java#L23-L56)
- [OrderDao.java#L23-L64](src/main/java/com/example/ordering/dao/OrderDao.java#L23-L64)

---

## 潜在风险

### 风险一：事务提交后回查订单使用新连接，存在数据不一致与性能问题

**位置**：[OrderService.java#L61-L64](src/main/java/com/example/ordering/service/OrderService.java#L61-L64)

`createOrder` 在事务提交后，调用 `orderDao.listByUser(userId, true)` 回查刚创建的订单。该方法从连接池获取一个**新连接**执行查询，存在以下问题：

- **数据可见性风险**：在数据库主从复制架构下，新连接可能路由到从库，而主从同步存在延迟，导致新创建的订单在从库上尚不可见，触发 `orElseThrow` 抛出 500 错误（"订单创建成功但查询失败"）。订单实际已持久化，但用户收到失败响应。
- **性能浪费**：`listByUser` 查询该用户的**全部订单**及其明细，再在 Java 侧通过 `stream().filter()` 按 `orderId` 过滤。当用户历史订单较多时，产生不必要的数据库负载和内存开销。应直接按 `orderId` 查询单条订单。

### 风险二：`FOR UPDATE` 仅锁定购物车行，菜单价格未加锁，存在价格篡改窗口

**位置**：[CartDao.java#L27-L34](src/main/java/com/example/ordering/dao/CartDao.java#L27-L34)

`listByUserForUpdate` 的 SQL 为 `SELECT ... FROM cart_items c JOIN menu_items m ... FOR UPDATE`。在 MySQL InnoDB 中，`FOR UPDATE` 仅对 `cart_items` 的行加锁，JOIN 的 `menu_items` 行**不会被锁定**。这意味着：

- 管理员在用户下单过程中修改菜品价格，由于 `FOR UPDATE` 是当前读（latest committed read），如果价格修改事务先提交，本事务可能读到**已变更的价格**，导致用户被收取与购物车页面展示不一致的金额。
- 更严重的是，用户在购物车页面看到的价格（通过 `listByUser` 不带 FOR UPDATE 的快照读获取）与下单时计算总价使用的价格可能不同，造成"标价与实付不一致"的业务纠纷。

### 风险三：无幂等保护，网络异常可能导致用户状态不一致

**位置**：[OrderService.java#L57-L64](src/main/java/com/example/ordering/service/OrderService.java#L57-L64)

下单接口没有幂等键（idempotency key）或去重机制。若客户端在服务端成功提交事务后、响应返回前发生网络中断：

- 用户收到超时或连接错误，无法确认订单是否创建成功
- 用户重新提交时，购物车已被清空，收到"购物车为空"错误
- 结果是：订单已创建但用户不知情，可能导致重复下单（通过重新加购）或投诉

建议在请求中引入幂等令牌，或在订单表中增加基于 `userId + 幂等键` 的唯一约束来防止重复创建。

### 风险四：仅检查菜品上下架状态，无库存数量扣减，存在超卖风险

**位置**：[OrderService.java#L47-L54](src/main/java/com/example/ordering/service/OrderService.java#L47-L54)

下单校验仅检查 `item.isAvailable()` 布尔标志，未对菜品库存数量进行校验与扣减。当多用户并发下单同一限量菜品时，所有请求均可通过校验并成功创建订单，导致超卖。应在下单事务中对 `menu_items` 的库存字段执行 `UPDATE ... SET stock = stock - ? WHERE stock >= ?` 并检查影响行数，确保库存扣减的原子性。
