# 运行与排错手册

## 1. 本地运行（非容器）

1. 执行 SQL 初始化：`sql/schema.sql` -> `sql/seed.sql`
2. 设置 `DB_URL/DB_USER/DB_PASSWORD`
3. 执行：`pnpm build`
4. 将 `target/online-ordering.war` 部署到 Tomcat `webapps/ROOT.war`

## 2. Docker 运行

```bash
cp .env.example .env
docker compose up --build
```

访问：`http://localhost:8080/login`

默认测试账号（仅本地环境）：

- 管理员：`admin` / `Admin@123`
- 普通用户：`test_user` / `User@123`

若此前已经初始化过旧数据库卷，建议先重建：

```bash
docker compose down -v --remove-orphans
docker compose up --build
```

## 3. 常见问题

- 数据库连接失败
  - 检查 `.env` 的 `DB_URL/DB_USER/DB_PASSWORD`
  - 检查 MySQL 是否已启动并通过健康检查
  - 检查 `DB_URL` 是否包含 `useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci`

- 页面返回 401
  - 确认先完成登录
  - 浏览器同源访问，保持 Session Cookie

- 中文乱码（菜单/订单出现 `é¦™` 等异常字符）
  - 确认 `docker-compose.yml` 中 MySQL 启动参数包含 `--character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci`
  - 确认 `DataSourceProvider` 默认 JDBC URL 和连接初始化 SQL 已启用 UTF-8 / utf8mb4
  - 对旧环境执行 `docker compose down -v` 后重建容器并重新初始化数据

- 菜品图片不可见
  - 检查静态目录 `src/main/webapp/assets/images/menu/` 是否存在图片文件
  - 检查数据库 `menu_items.image_url` 是否为 `/assets/images/menu/*.jpg` 本地路径

- 构建失败
  - 检查 JDK 版本（建议 17+，课程环境可用 24）
  - 执行 `pnpm clean` 后重试

## 4. E2E 自动化回归

```bash
pnpm exec playwright install chromium
pnpm test:e2e
```

如果需要保留容器用于调试：

```bash
E2E_KEEP_CONTAINERS=1 pnpm test:e2e
```

## 5. 资源目录约定

- 菜品静态图片目录：`src/main/webapp/assets/images/menu/`
- 主题样式目录：`src/main/webapp/assets/css/theme.css`
- 新增种子菜品时，推荐先下载图片到本地后再写入 `image_url`，避免外链失效。
