# 在线订餐系统（MVP） MVP 规划
*campus-online-ordering-mvp*

## 项目概述

**描述**: 面向课程设计的在线订餐系统 MVP：用户可注册/登录，浏览菜品、加入购物车并下单；管理员可维护菜品与处理订单。重点完成 MySQL 数据库设计与 Java Web 应用实现，使用 JDK 24 + Tomcat 10+ + MySQL 8.0，采用会话(Session)鉴权与 JDBC 访问数据库。

**目标用户**:
- 普通用户：在线浏览菜单、下单、查看订单
- 管理员：维护菜单、查看并处理订单

## 原始需求

> 在线订餐系统 

课程设计的目的和要求： 
《数据库应用系统》课程设计是软件工程专业非常重要的实践性环节之一，是学完《数 
据库原理及应用》课程后一次全面的综合练习。本课程设计主要在于巩固学生对数据库基本 
原理和基础理论的理解，掌握数据库应用系统设计开发的基本方法，进一步提高学生综合运 
用所学知识的能力本次课程设计的重点是完成数据库的设计和实现，兼顾应用程序开发。数据库管理系统不限。 

jdk24  tomcat10+  mysql8.0

## 评分信息

| 维度 | 分值 |
|------|------|
| 等级 | B |
| 总分 | 3.65 |
| 类型权重 | 3.25 |
| 加权总分 | 11.86 |
| 清晰度 | 4 |
| 复杂度 | 3.0 |
| 验证难度 | 1.8 |

## 技术栈

- **前端**: JSP + JSTL + Bootstrap 5 + 原生 JavaScript(基于 fetch 调用后端 API)
- **后端**: Java (JDK 24) + Tomcat 10+ + Servlet + JDBC + JSON(手写/轻量库) + HttpSession
- **数据库**: MySQL 8.0 (InnoDB, 外键约束, 索引)
- **选型理由**: 与课程环境(JDK24/Tomcat10/MySQL8)直接匹配；Servlet+JDBC便于展示数据库设计、SQL与事务；JSP适合快速完成可运行的全栈页面与表单；Session鉴权无需依赖付费或外部服务。

## 核心功能

### 用户与鉴权 (P0)

- [ ] 用户注册：用户名唯一校验，密码加密存储
- [ ] 用户登录/退出：基于 HttpSession 保存登录态
- [ ] 角色区分：USER/ADMIN 控制访问管理员功能

### 菜单浏览 (P0)

- [ ] 菜品列表展示：名称、价格、描述、是否可售
- [ ] 菜品搜索/筛选：按关键字(名称)过滤
- [ ] 菜品详情查看：展示完整描述与图片链接(可选)

### 购物车与下单 (P0)

- [ ] 购物车管理：加入、修改数量、删除、清空
- [ ] 提交订单：填写联系人/电话/地址并生成订单与订单明细
- [ ] 订单列表与详情：查看历史订单与状态

### 管理员后台 (P1)

- [ ] 菜品管理：新增/编辑/上下架(可售状态)
- [ ] 订单管理：查看订单列表与详情
- [ ] 订单状态流转：PLACED -> CONFIRMED -> COMPLETED / CANCELED

## 页面结构

| 路由 | 页面 | 描述 |
|------|------|------|
| `/login` | 登录 | 用户登录进入系统 |
| `/register` | 注册 | 新用户注册账号 |
| `/menu` | 菜单 | 浏览与搜索菜品并加入购物车 |
| `/cart` | 购物车 | 管理购物车商品并发起下单 |
| `/orders` | 我的订单 | 查看用户订单列表与订单详情 |
| `/admin/menu` | 后台-菜品管理 | 管理员维护菜品信息与可售状态 |
| `/admin/orders` | 后台-订单管理 | 管理员查看并处理订单状态 |

## 数据模型

### User

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK AI | 用户主键 |
| username | VARCHAR(50) UNIQUE NOT NULL | 登录用户名，唯一 |
| password_hash | VARCHAR(255) NOT NULL | 密码哈希(如 SHA-256+salt 或 PBKDF2) |
| role | ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER' | 用户角色，用于权限控制 |
| created_at | DATETIME NOT NULL | 创建时间 |

### MenuItem

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK AI | 菜品主键 |
| name | VARCHAR(100) NOT NULL | 菜品名称 |
| description | VARCHAR(500) NOT NULL | 菜品描述 |
| price | DECIMAL(10,2) NOT NULL | 菜品价格 |
| image_url | VARCHAR(255) NOT NULL | 图片链接(可用占位链接) |
| is_available | TINYINT(1) NOT NULL DEFAULT 1 | 是否可售(1可售/0下架) |
| created_at | DATETIME NOT NULL | 创建时间 |

### CartItem

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK AI | 购物车项主键 |
| user_id | BIGINT UNSIGNED NOT NULL | 所属用户ID(User.id) |
| menu_item_id | BIGINT UNSIGNED NOT NULL | 菜品ID(MenuItem.id) |
| quantity | INT NOT NULL | 数量(>=1) |
| created_at | DATETIME NOT NULL | 加入时间 |

### Order

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK AI | 订单主键 |
| user_id | BIGINT UNSIGNED NOT NULL | 下单用户ID(User.id) |
| status | ENUM('PLACED','CONFIRMED','COMPLETED','CANCELED') NOT NULL DEFAULT 'PLACED' | 订单状态 |
| total_amount | DECIMAL(10,2) NOT NULL | 订单总金额(由明细汇总) |
| contact_name | VARCHAR(50) NOT NULL | 联系人姓名 |
| contact_phone | VARCHAR(30) NOT NULL | 联系人电话 |
| delivery_address | VARCHAR(255) NOT NULL | 送餐地址 |
| created_at | DATETIME NOT NULL | 下单时间 |
| updated_at | DATETIME NOT NULL | 更新时间(状态变更等) |

### OrderItem

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT UNSIGNED PK AI | 订单明细主键 |
| order_id | BIGINT UNSIGNED NOT NULL | 所属订单ID(Order.id) |
| menu_item_id | BIGINT UNSIGNED NOT NULL | 菜品ID(MenuItem.id)，用于追溯 |
| item_name_snapshot | VARCHAR(100) NOT NULL | 下单时菜品名称快照(避免后续改名影响历史) |
| unit_price_snapshot | DECIMAL(10,2) NOT NULL | 下单时单价快照 |
| quantity | INT NOT NULL | 购买数量 |
| line_total | DECIMAL(10,2) NOT NULL | 行小计=unit_price_snapshot*quantity |

## API 端点

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | `none` | 用户注册：username/password；创建 USER 角色账号 |
| `POST` | `/api/auth/login` | `none` | 用户登录：创建 HttpSession 并返回当前用户信息 |
| `POST` | `/api/auth/logout` | `required` | 退出登录：销毁会话 |
| `GET` | `/api/menu-items` | `required` | 获取菜品列表：支持 query 参数 q(按名称模糊查询)，仅返回可售菜品给普通用户 |
| `GET` | `/api/cart` | `required` | 获取当前用户购物车明细与金额汇总 |
| `POST` | `/api/cart/items` | `required` | 加入购物车：menuItemId, quantity(默认1)；同菜品则累加数量 |
| `PUT` | `/api/cart/items/{id}` | `required` | 更新购物车项数量：quantity；quantity<=0 则删除该项 |
| `DELETE` | `/api/cart/items/{id}` | `required` | 删除购物车项 |
| `POST` | `/api/orders` | `required` | 创建订单：contactName/contactPhone/deliveryAddress；从购物车生成 Order 与 OrderItem(事务)，并清空购物车 |
| `GET` | `/api/orders` | `required` | 获取当前用户订单列表(含基础字段，可选带明细开关 includeItems=true) |
| `PUT` | `/api/admin/menu-items/{id}` | `required` | 管理员编辑菜品：name/description/price/imageUrl/isAvailable |
| `POST` | `/api/admin/menu-items` | `required` | 管理员新增菜品 |
| `GET` | `/api/admin/orders` | `required` | 管理员查看订单列表：可按 status 筛选 |
| `PUT` | `/api/admin/orders/{id}/status` | `required` | 管理员更新订单状态：status in [CONFIRMED, COMPLETED, CANCELED]，并写入 updated_at |

## 验收标准

### AC-CORE-001 (core)

- **Given**: 用户已完成注册且账号有效
- **When**: 用户在登录页输入正确用户名与密码并提交
- **Then**: 系统创建会话并跳转到菜单页，页面显示“退出/购物车/订单”入口

### AC-CORE-002 (core)

- **Given**: 用户已登录且菜单存在至少1个可售菜品
- **When**: 用户在菜单页点击“加入购物车”并进入购物车页提交订单(填写联系人/电话/地址)
- **Then**: 系统生成订单与订单明细，购物车被清空，订单列表出现新订单且金额与明细汇总一致

### AC-EDGE-001 (edge)

- **Given**: 用户已登录且购物车中已有某菜品数量为1
- **When**: 用户将该菜品数量减少到0或输入非正整数并提交更新
- **Then**: 系统阻止非法数量：数量为0时删除该项；非正整数时返回校验错误并保持原数量

### AC-ERROR-001 (error)

- **Given**: 用户未登录或会话已失效
- **When**: 用户直接访问 /cart 或调用 /api/orders 创建订单接口
- **Then**: 系统返回未授权(页面跳转至登录页或 API 返回 401)，且不产生任何订单数据

### AC-USABILITY-001 (usability)

- **Given**: 用户处于菜单页
- **When**: 用户在搜索框输入关键字并提交/触发搜索
- **Then**: 列表仅展示名称包含关键字的菜品，且搜索条件在页面上可见并可清空恢复全部

### AC-CORE-003 (core)

- **Given**: 管理员账号已登录且存在订单状态为 PLACED 的订单
- **When**: 管理员在后台订单管理页将订单状态更新为 CONFIRMED
- **Then**: 订单状态被持久化更新，用户在“我的订单”页面刷新后可见状态变化与更新时间变化

### AC-ERROR-002 (error)

- **Given**: 管理员在后台新增或编辑菜品时输入价格为负数或非数字
- **When**: 管理员提交保存
- **Then**: 系统返回校验错误提示，不写入数据库且不影响现有菜品数据

## 不在 MVP 范围内

- 在线支付、退款、第三方支付接口
- 多商家/多门店、地理位置与配送范围计算
- 配送员端、实时配送追踪
- 优惠券、满减、会员积分、评价系统
- 短信/邮件通知、第三方登录
- 复杂报表与数据分析、打印小票
- 高并发分布式架构、消息队列、缓存系统

## 实现里程碑

### Phase 1 - 数据库设计与初始化

- [ ] 完成 E-R 设计并落表：User/MenuItem/CartItem/Order/OrderItem
- [ ] 建立主键、外键、必要索引(如 CartItem.user_id, Order.user_id, OrderItem.order_id)
- [ ] 准备初始化数据：1个管理员账号、若干菜品
- [ ] 编写 SQL 脚本：schema.sql + seed.sql

**验收**: ['在 MySQL 8.0 成功创建所有表且外键生效', '可通过 SQL 插入并查询到种子数据', '订单与明细、购物车与用户/菜品之间关联查询正确']

### Phase 2 - 后端 Servlet + JDBC API

- [ ] 搭建 Tomcat Web 项目结构，配置数据库连接(JDBC)与统一异常/JSON响应
- [ ] 实现鉴权：注册/登录/退出，Session 存储 userId 与 role
- [ ] 实现菜单与购物车 API：列表、加入、修改、删除
- [ ] 实现下单事务：从购物车生成订单与明细并清空购物车(同一事务)
- [ ] 实现管理员 API：菜品增改、订单列表与状态更新

**验收**: ['Postman/浏览器调用 API 可完成完整链路：登录->加购->下单->查询订单', '未登录访问受保护 API 返回 401；非管理员访问管理 API 返回 403', '下单接口具备事务一致性：任一步失败不产生半成品订单且购物车不被清空']

### Phase 3 - JSP 页面与端到端联调

- [ ] 实现 JSP 页面：登录/注册/菜单/购物车/我的订单/后台菜品/后台订单
- [ ] 页面通过 fetch 调用后端 API，展示错误提示与基本交互
- [ ] 输入校验：必填、价格与数量合法性、用户名唯一提示
- [ ] 完成基本验收用例测试与演示脚本

**验收**: ['普通用户可在页面端完成浏览、加购、下单、查看订单', '管理员可在页面端完成新增/编辑/上下架菜品与处理订单状态', '所有验收标准(acceptance_criteria)均可在页面端复现并通过']
