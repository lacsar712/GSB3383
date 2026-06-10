# E2E 覆盖矩阵

## 页面覆盖

- `/login`：错误密码、正确登录
- `/register`：密码不一致、注册成功、重复用户名
- `/menu`：关键字搜索、无结果空态、清空恢复、可售菜品加购
- `/cart`：数量负数校验、非整数前端拦截、数量更新、数量置零删除、删除按钮路径、空购物车下单失败、正常下单
- `/orders`：空订单列表、订单列表渲染、订单明细与状态展示
- `/admin/menu`：新增菜品、价格负数/非数字校验、合法更新与上下架
- `/admin/orders`：状态筛选、无匹配空态、确认订单、完成订单、非法状态值、非法状态流转

## 权限与错误分支

- 根路由 `/`：未登录跳转 `/login`，已登录跳转 `/menu`
- 退出登录后：受保护页面重定向，受保护 API 返回 401
- 未登录访问 `/cart`、`/orders` 页面跳转登录
- 未登录调用 `/api/orders` 返回 401
- 普通用户访问 `/admin/menu` 返回 403
- 普通用户调用 `/api/admin/orders` 返回 403

## 边界与状态流转

- 购物车数量：`<0` 报错、`=0` 删除、`>0` 更新
- 购物车数量：非整数（如 `1.5`）前端拦截
- 购物车删除：删除后再次删除返回 `404/NOT_FOUND`
- 菜品价格：负数/非数字报错
- 下架菜品：接口加购返回 `400/MENU_ITEM_UNAVAILABLE`
- 订单状态：`PLACED -> CONFIRMED -> COMPLETED` 正向流转
- 订单状态非法流转：`COMPLETED -> CONFIRMED` 返回 `INVALID_STATUS_TRANSITION`
- 订单状态非法值：`INVALID_STATUS` 返回 400

## 四层断言

关键场景包含以下断言：

1. API 层：HTTP 状态码与业务错误码
2. 状态层：订单状态变更、购物车项变化
3. 渲染层：列表项、错误提示、状态徽章可见
4. 交互层：按钮点击、输入框编辑、筛选触发

## 用例文件

- `e2e/specs/auth-pages.spec.mjs`
- `e2e/specs/menu-cart-orders.spec.mjs`
- `e2e/specs/admin-pages.spec.mjs`
- `e2e/specs/branch-boundary-pages.spec.mjs`
