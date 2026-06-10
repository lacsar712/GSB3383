# 订单创建流程分析

## 调用链路

### 总体架构

用户下单流程遵循经典的三层架构：`Web 层 → Service 层 → DAO 层`，整体调用链如下：

```
POST /api/orders
    ↓
AuthFilter（认证拦截）
    ↓
ApiServlet.handleCreateOrder()  [Web 层]
    ↓
OrderService.createOrder()      [Service 层]
    ├── CartDao.listByUserForUpdate()   [DAO 层]
    ├── CartDao.sumTotal()              [DAO 层]
    ├── OrderDao.createOrder()          [DAO 层]
    ├── OrderDao.insertOrderItems()     [DAO 层]
    ├── CartDao.clearByUser()           [DAO 层]
    └── OrderDao.listByUser()           [DAO 层]
    ↓
返回 OrderDTO
```

---

### Web 层

#### 1. AuthFilter — 认证拦截

- **文件**：[AuthFilter.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/filter/AuthFilter.java)
- **入口**：`doFilter()` [第 26-47 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/filter/AuthFilter.java#L26-L47)
- **作用**：拦截 `/api/*` 路径，校验用户是否已登录。未登录时返回 401 错误。
- **白名单**：`/api/auth/login` 和 `/api/auth/register` 无需认证。

#### 2. ApiServlet — 请求分发与处理

- **文件**：[ApiServlet.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/servlet/ApiServlet.java)
- **入口方法**：`handleCreateOrder()` [第 208-218 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/servlet/ApiServlet.java#L208-L218)
- **触发条件**：`POST /api/orders`

**处理流程**：
1. 调用 `requireCurrentUserId()` 从 Session 中获取当前登录用户 ID
2. 解析 JSON 请求体，提取 `contactName`、`contactPhone`、`deliveryAddress` 三个必填字段
3. 调用 `OrderService.createOrder()` 执行业务逻辑
4. 将返回的 `OrderDTO` 序列化为 JSON 响应

**辅助方法**：
- `requireCurrentUserId()` [第 276-282 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/servlet/ApiServlet.java#L276-L282)：通过 `SessionUtil.currentUserId()` 获取用户 ID，未登录则抛出 401
- `getRequiredString()` [第 284-290 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/web/servlet/ApiServlet.java#L284-L290)：校验必填字符串参数

---

### Service 层

#### OrderService — 核心业务逻辑

- **文件**：[OrderService.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java)
- **核心方法**：`createOrder()` [第 30-77 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L30-L77)

**完整执行流程**：

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1 | 参数校验 | 调用 `ValidationUtil.requireLength()` 分别校验联系人（1-50字）、电话（1-30字）、地址（1-255字） |
| 2 | 开启事务 | 从连接池获取连接，设置 `autoCommit = false` |
| 3 | 查询购物车 | 调用 `cartDao.listByUserForUpdate()`，使用 `FOR UPDATE` 行锁锁定购物车数据 |
| 4 | 购物车校验 | 空购物车 → 抛 400；数量 ≤ 0 → 抛 400；菜品已下架 → 抛 400 |
| 5 | 计算总价 | 调用 `cartDao.sumTotal()` 累加所有购物车项的 `lineTotal` |
| 6 | 创建订单 | 调用 `orderDao.createOrder()` 插入订单主记录，返回订单 ID |
| 7 | 插入订单项 | 调用 `orderDao.insertOrderItems()` 批量插入订单明细，保存菜品名称和单价快照 |
| 8 | 清空购物车 | 调用 `cartDao.clearByUser()` 删除该用户所有购物车项 |
| 9 | 提交事务 | `conn.commit()` |
| 10 | 查询订单 | 调用 `orderDao.listByUser()` 查询用户所有订单，过滤出刚创建的订单返回 |

**异常处理**：
- `ApiException`：业务校验失败，回滚事务后重新抛出
- 其他 `Exception`：包装为 500 错误，回滚事务
- `SQLException`：获取连接失败，包装为 500 错误
- `finally` 块：恢复 `autoCommit = true`

---

### DAO 层

#### 1. CartDao — 购物车数据访问

- **文件**：[CartDao.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java)

| 方法 | 位置 | 作用 |
|------|------|------|
| `listByUserForUpdate()` | [第 23-25 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L23-L25) | 查询用户购物车，关联 `menu_items` 表，使用 `FOR UPDATE` 加行锁 |
| `sumTotal()` | [第 115-119 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L115-L119) | Java 流式计算购物车总价（纯内存计算，不访问数据库） |
| `clearByUser(Connection, long)` | [第 105-113 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L105-L113) | 事务内清空用户购物车 |

**SQL 关键点**：
- `listByUserForUpdate` 关联查询 `cart_items` 和 `menu_items`，计算 `line_total = price * quantity`
- `FOR UPDATE` 仅锁定 `cart_items` 表的行，不锁定 `menu_items` 表

#### 2. OrderDao — 订单数据访问

- **文件**：[OrderDao.java](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java)

| 方法 | 位置 | 作用 |
|------|------|------|
| `createOrder()` | [第 23-47 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L23-L47) | 插入 `orders` 表，初始状态 `PLACED`，返回自增主键 |
| `insertOrderItems()` | [第 49-64 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L49-L64) | 批量插入 `order_items` 表，保存菜品名称和单价快照 |
| `listByUser()` | [第 66-81 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L66-L81) | 查询用户所有订单，可关联查询订单项 |

**数据快照设计**：
- `order_items` 表保存 `item_name_snapshot` 和 `unit_price_snapshot`
- 目的：订单创建后，即使菜品改名或调价，订单数据保持不变

---

## 潜在风险

### 风险 1：创建订单后查询效率低下（性能风险）

**位置**：[OrderService.createOrder() 第 61-64 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L61-L64)

**问题描述**：
为了返回刚创建的完整订单信息（包含订单项），代码调用 `orderDao.listByUser(userId, true)` 查询该用户的**所有**订单，然后在 Java 内存中通过 `stream().filter()` 过滤出目标订单。

```java
return orderDao.listByUser(userId, true).stream()
        .filter(order -> order.id() == orderId)
        .findFirst()
        .orElseThrow(...);
```

**风险分析**：
- 当用户历史订单量较大时，会加载大量不必要的数据
- 额外的数据库 IO、网络传输和内存开销
- 两次查询（创建 + 查询列表）且查询列表的成本随订单量线性增长
- `OrderDao.listByUser()` 内部还会关联查询 `order_items` 表，进一步放大性能问题

**修复建议**：
在 `OrderDao` 中新增 `findById(Connection, long, boolean)` 方法，根据订单 ID 直接查询单条记录，创建订单后复用同一事务内的连接进行查询。

---

### 风险 2：菜品价格/上下架状态的并发一致性问题（数据一致性风险）

**位置**：[CartDao.listByUser() 第 27-56 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/CartDao.java#L27-L56)

**问题描述**：
虽然购物车查询使用了 `FOR UPDATE` 行锁，但该锁仅作用于 `cart_items` 表，不会锁定关联的 `menu_items` 表。这意味着在事务执行期间：

```sql
SELECT c.id, c.menu_item_id, m.name, m.price, c.quantity, ...
FROM cart_items c 
JOIN menu_items m ON m.id = c.menu_item_id 
WHERE c.user_id = ? 
FOR UPDATE
```

**可能发生的并发场景**：
1. 用户 A 开始下单事务，查询购物车（锁定 cart_items 行），此时菜品价格为 10 元，状态为上架
2. 管理员修改该菜品：价格改为 20 元，或设置为下架
3. 用户 A 的事务继续执行，使用的是第 1 步查询时的价格/状态快照完成下单

**风险分析**：
- **价格不一致**：下单价格可能与当前菜品价格不一致（虽然使用了查询时的快照，但如果事务隔离级别是 READ COMMITTED，且中间发生了其他读取，情况会更复杂）
- **下架菜品仍可下单**：如果在查询购物车之后、校验 `isAvailable` 之前菜品被下架，校验可能无法发现（取决于隔离级别）
- 在 MySQL 默认的 REPEATABLE READ 隔离级别下，`FOR UPDATE` 是当前读，会读取最新已提交数据，但只锁 `cart_items` 不锁 `menu_items`，后续的 `menu_items` 变更不会被阻塞

**修复建议**：
- 将 `FOR UPDATE` 改为对 `menu_items` 表也加锁（例如使用 `FOR UPDATE` 或 `LOCK IN SHARE MODE`）
- 或在校验菜品状态时，单独对 `menu_items` 表执行 `SELECT ... FOR UPDATE` 确保锁定
- 考虑在 `cart_items` 表中冗余存储价格快照，加入购物车时即确定价格

---

### 风险 3：批量插入订单项未校验执行结果（数据完整性风险）

**位置**：[OrderDao.insertOrderItems() 第 49-64 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/dao/OrderDao.java#L49-L64)

**问题描述**：
使用 JDBC 批量插入 `executeBatch()` 时，未检查返回结果数组来确认每一条记录是否插入成功。

```java
try (PreparedStatement stmt = conn.prepareStatement(sql)) {
    for (CartItemDTO item : cartItems) {
        // ... 设置参数
        stmt.addBatch();
    }
    stmt.executeBatch();  // 未检查返回值
}
```

**风险分析**：
- `executeBatch()` 返回 `int[]` 数组，每个元素对应一条 SQL 的执行结果
- 如果部分插入失败（如数据超长、约束违反等），可能导致订单主记录存在但订单项缺失
- 虽然在同一事务中，通常整个批处理会一起回滚，但在某些驱动或配置下可能出现部分成功的情况
- 缺乏结果校验使得问题难以排查

**修复建议**：
- 捕获 `executeBatch()` 的返回数组，校验成功条数与预期是否一致
- 或者显式校验 `BatchUpdateException`

---

### 风险 4：电话号码仅校验长度，未校验格式（业务风险）

**位置**：[OrderService.createOrder() 第 37 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L37)

**问题描述**：
联系电话仅通过 `ValidationUtil.requireLength("联系电话", contactPhone, 1, 30)` 校验长度，未做任何格式验证。

**风险分析**：
- 用户可输入任意字符串（如 "abc"、"!!! " 等）作为联系电话
- 配送时无法联系用户，影响订单履约
- 缺乏基础的格式校验可能导致垃圾数据入库

**修复建议**：
- 增加手机号格式正则校验（如中国大陆手机号 `^1[3-9]\d{9}$`）
- 或至少校验是否包含有效数字
- 根据业务需求支持固定电话、国际号码等格式

---

### 风险 5：事务中新插入的购物车项会被误删除（并发边界风险）

**位置**：[OrderService.createOrder() 第 43 行 + 第 59 行](file:///d:/work/document/code/modelX/GSB0608/label-3383/GSB3383/src/main/java/com/example/ordering/service/OrderService.java#L43-L59)

**问题描述**：
`listByUserForUpdate()` 使用 `FOR UPDATE` 锁定查询结果中已存在的 `cart_items` 行，但**无法阻止新的购物车项被插入**（因为插入的是新行，不在原有结果集内）。

**并发场景**：
1. 用户在设备 A 上下单，事务开始，锁定当前 2 个购物车项
2. 同一用户在设备 B 上添加第 3 个菜品到购物车（INSERT 新行，不受现有行锁影响）
3. 设备 A 的下单事务继续执行，`clearByUser()` 会删除**所有**购物车项，包括第 3 个
4. 用户在设备 B 上发现刚添加的菜品"消失"了，且未包含在订单中

**风险分析**：
- 用户体验问题：添加的菜品莫名消失
- 业务一致性问题：清空购物车的语义应该是"清空参与下单的购物车项"，而非"清空所有"
- 多端同时操作时容易出现数据丢失感

**修复建议**：
- 记录下单时涉及的购物车项 ID，只删除这些项
- 或者在清空后检查是否仍有购物车项，提示用户
- 更严格的方案：对用户级加锁（如使用 `SELECT ... FOR UPDATE` 锁定用户表的对应行），但可能影响并发性能
