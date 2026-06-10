# 验收说明（AC 对照）

## AC-CORE-001

- 用例：注册后登录
- 预期：登录成功后跳转 `/menu`，页面可见「退出/购物车/订单」
- 证据：`/login` 页面提交成功后重定向；`menu.jsp` 导航栏包含对应入口

## AC-CORE-002

- 用例：菜单加购 -> 购物车下单
- 预期：生成订单与明细，购物车清空，订单列表可见新订单
- 证据：`OrderService#createOrder` 单事务写入 `orders + order_items` 并清空 `cart_items`

## AC-EDGE-001

- 用例：购物车数量更新为 0 或非正整数
- 预期：0 删除条目；负数/非法值拒绝
- 证据：`CartService#updateQuantity` 与 `ApiServlet#getRequiredInt` 校验逻辑

## AC-ERROR-001

- 用例：未登录访问 `/cart` 或 `POST /api/orders`
- 预期：页面跳转登录/API 返回 401
- 证据：`AuthFilter` 对页面与 API 分支处理

## AC-USABILITY-001

- 用例：菜单关键字搜索并清空
- 预期：显示过滤结果，支持恢复全部
- 证据：`menu.jsp` 搜索与清空逻辑 + `GET /api/menu-items?q=`

## AC-CORE-003

- 用例：管理员将订单 `PLACED` 更新为 `CONFIRMED`
- 预期：用户侧刷新可见状态与更新时间变化
- 证据：`OrderService#updateOrderStatus` + `OrderDao#updateStatus`

## AC-ERROR-002

- 用例：管理员输入负数或非数字价格
- 预期：返回校验错误，不写库
- 证据：`MenuService` 使用 `ValidationUtil#requirePositiveDecimal`

## AC-I18N-001（新增）

- 用例：登录后请求 `GET /api/menu-items`
- 预期：菜名/描述返回正常中文（如“黑椒牛肉饭”），不出现乱码
- 证据：JDBC URL 启用 UTF-8 参数、MySQL utf8mb4 启动参数、连接初始化 SQL `SET NAMES utf8mb4`

## AC-ASSET-001（新增）

- 用例：查询菜单并检查 `imageUrl`
- 预期：图片路径全部指向本地静态资源 `/assets/images/menu/*.jpg`
- 证据：`sql/seed.sql` 已切换本地路径；实际文件位于 `src/main/webapp/assets/images/menu/`
