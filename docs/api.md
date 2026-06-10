# API 文档

统一响应格式：

- 成功：`{ "success": true, "data": ... }`
- 失败：`{ "success": false, "error": { "code": "...", "message": "..." } }`

## 鉴权

- `POST /api/auth/register` 注册
- `POST /api/auth/login` 登录（创建 Session）
- `POST /api/auth/logout` 退出登录（销毁 Session）

## 菜单

- `GET /api/menu-items?q=关键字`
  - 普通用户仅返回可售菜品
  - 管理员返回全部菜品

## 购物车

- `GET /api/cart`
- `POST /api/cart/items`
  - body: `{ "menuItemId": 1, "quantity": 1 }`
- `PUT /api/cart/items/{id}`
  - body: `{ "quantity": 2 }`
  - `quantity = 0` 表示删除
- `DELETE /api/cart/items/{id}`

## 订单

- `POST /api/orders`
  - body: `{ "contactName": "张三", "contactPhone": "138...", "deliveryAddress": "教学楼 A101" }`
- `GET /api/orders?includeItems=true`

## 管理员

- `POST /api/admin/menu-items`
- `PUT /api/admin/menu-items/{id}`
- `GET /api/admin/orders?status=PLACED&includeItems=true`
- `PUT /api/admin/orders/{id}/status`
  - body: `{ "status": "CONFIRMED" }`
  - 状态流转：`PLACED -> CONFIRMED/CANCELED`，`CONFIRMED -> COMPLETED/CANCELED`

## 错误码

- `UNAUTHORIZED`：未登录
- `FORBIDDEN`：权限不足
- `VALIDATION_ERROR`：参数不合法
- `NOT_FOUND`：资源不存在
- `USERNAME_EXISTS`：用户名已存在
- `INVALID_STATUS_TRANSITION`：订单状态流转不合法
