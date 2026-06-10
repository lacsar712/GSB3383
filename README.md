# 在线订餐系统（MVP）

基于 `Java Servlet + JSP + JDBC + MySQL 8.0 + Tomcat 10` 的课程设计项目，覆盖用户端与管理员端完整链路。

## 原始需求

> 在线订餐系统

> 课程设计的目的和要求：
> 《数据库应用系统》课程设计是软件工程专业非常重要的实践性环节之一，是学完《数
> 据库原理及应用》课程后一次全面的综合练习。本课程设计主要在于巩固学生对数据库基本
> 原理和基础理论的理解，掌握数据库应用系统设计开发的基本方法，进一步提高学生综合运
> 用所学知识的能力本次课程设计的重点是完成数据库的设计和实现，兼顾应用程序开发。数据库管理系统不限。

> jdk24 tomcat10+ mysql8.0

## 功能范围

- P0：注册/登录/退出、菜单浏览与搜索、购物车管理、下单、我的订单
- P1：管理员菜品管理（新增/编辑/上下架）、订单管理（状态流转）

## 技术栈

- 后端：Java 17+（兼容 JDK 24）、Servlet、JDBC、HikariCP
- 前端：JSP + JSTL + Bootstrap 5 + 原生 JavaScript
- 数据库：MySQL 8.0
- 容器：Docker Compose（非 Alpine 镜像）
- 工程命令入口：pnpm

## 代码架构

- 架构形态：单体 Web 应用（WAR 部署），按 `web -> service -> dao -> domain` 分层组织。
- 控制层：`src/main/java/com/example/ordering/web` 下的 Servlet 负责路由分发、参数接收与 JSON 输出。
- 鉴权层：Filter 负责未登录拦截与管理员权限控制，页面路由与 API 路由分别处理重定向/状态码。
- 业务层：`service` 封装注册登录、购物车、下单、订单流转、管理员管理等核心业务规则。
- 数据层：`dao` 基于 JDBC + `PreparedStatement` 实现 CRUD，统一通过连接池访问 MySQL。
- 视图层：`src/main/webapp/WEB-INF/views` 中 JSP 负责页面骨架，前端通过 `fetch` 调用后端 API。
- 资源层：静态文件在 `src/main/webapp/assets`，包含主题样式、脚本与本地化菜单图片。
- 测试层：`src/test` 负责后端测试，`e2e` 目录使用 Playwright 执行端到端流程回归。

## 技术细节

- 会话鉴权：基于 `HttpSession` 维护登录态，区分普通用户与管理员角色权限。
- 密码安全：登录与注册流程采用带随机盐的 PBKDF2 哈希，避免明文存储密码。
- 输入校验：服务端对用户名、密码、价格、数量、订单状态流转进行二次校验并返回可读错误信息。
- 事务一致性：下单流程在单事务中完成“创建订单 + 写入明细 + 清空购物车”，失败即回滚。
- 接口契约：API 统一返回 `success/data/error` 结构，遵循 400/401/403/404/409 等状态语义。
- 数据库编码：JDBC URL 与 MySQL 服务端统一使用 `utf8mb4`，避免中文乱码与排序异常。
- 容器化部署：`Dockerfile` + `docker-compose.yml` 提供应用与 MySQL 一键启动，镜像均非 Alpine。
- 工程命令：通过 `pnpm` 统一封装 `lint/test/build`，底层调用 Maven 与 Playwright。

## 快速开始（本地）

1. 准备数据库：创建并初始化 `online_ordering` 数据库

```bash
mysql -uroot -p < sql/schema.sql
mysql -uroot -p < sql/seed.sql
```

2. 设置环境变量（可复制 `.env.example`）

```bash
export DB_URL='jdbc:mysql://localhost:3306/online_ordering?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci'
export DB_DRIVER='com.mysql.cj.jdbc.Driver'
export DB_USER='ordering'
export DB_PASSWORD='ordering123'
export DB_POOL_SIZE='10'
```

3. 构建并部署到 Tomcat（示例）

```bash
pnpm build
cp target/online-ordering.war "$CATALINA_HOME/webapps/ROOT.war"
```

4. 启动 Tomcat 后访问：`http://localhost:8080/login`

## 一键容器启动（推荐）

```bash
cp .env.example .env
docker compose up --build
```

启动后访问：`http://localhost:8080/login`

如果你在本次乱码修复前已经跑过旧版本数据库，请先重建数据卷再启动，避免旧字符集会话配置污染：

```bash
docker compose down -v --remove-orphans
docker compose up --build
```

默认测试账号（仅用于本地演示）：

- 管理员：`admin` / `Admin@123`
- 普通用户：`test_user` / `User@123`

## 工程命令

```bash
pnpm lint
pnpm test
pnpm test:e2e
pnpm build
```

首次执行 E2E 前请安装浏览器：

```bash
pnpm exec playwright install chromium
```

E2E 会自动执行：

1. `docker compose down -v --remove-orphans`
2. `docker compose up -d --build`
3. 等待 `http://127.0.0.1:8080/login` 就绪
4. 执行 Playwright 用例
5. 自动清理容器（可通过 `E2E_KEEP_CONTAINERS=1` 保留）

## 中文与图片说明

- 中文编码：应用层 JDBC 连接 + MySQL 服务端均固定为 `utf8mb4`，默认可避免菜单中文乱码。
- 菜品图片：种子数据全部使用本地静态资源路径 `/assets/images/menu/*.jpg`，不依赖线上外链。
- 图片溯源：见 `docs/image-sources.md`。

## 项目结构

```text
src/main/java/com/example/ordering
├── config      # 配置与数据源
├── dao         # JDBC 数据访问
├── domain      # 领域模型与 DTO
├── exception   # 业务异常
├── service     # 业务服务
├── util        # 工具类
└── web         # Servlet 与 Filter

src/main/webapp/WEB-INF/views  # JSP 页面
sql/                            # schema.sql / seed.sql
docs/                           # API、验收、运行手册
```

## 非目标（未实现）

- 在线支付/退款/第三方支付
- 优惠券、积分、评价、复杂报表
- 多商家、多门店、配送员端
