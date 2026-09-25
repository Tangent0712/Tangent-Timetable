# 生产部署指南

> 目标服务器：`<YOUR_SERVER_IP>`（宝塔面板 + Nginx + MySQL）
> 站点：**https://todo.tangent0712.top**
> 更新日期：2026-08-07

本项目「大切课程表」为前后端分离应用，已部署到生产环境。本文档说明部署架构、运维命令与更新流程，供接手者使用。

---

## 1. 架构总览

```
浏览器
  │  https://todo.tangent0712.top
  ▼
Nginx (宝塔)  ── 静态文件 /www/wwwroot/todo.tangent0712.top/  （React 前端）
  │  location /api/ 反向代理
  ▼
Spring Boot 后端 127.0.0.1:8200  （/opt/timetable/backend/）
  │
  ▼
MySQL 数据库 timetable  （独立库 + 专用账号）
```

- 前端：Vite + React + TS + Antd，构建产物为纯静态文件
- 后端：Spring Boot 3，端口 8200，systemd 托管
- 数据库：`timetable`，专用账号隔离，不影响服务器其他应用

---

## 2. SSH 连接

```bash
# 本地 ~/.ssh/config 已配置别名
ssh tangent-server          # admin@<YOUR_SERVER_IP>，免密 sudo
```

---

## 3. 服务管理

```bash
# 查看后端状态
ssh tangent-server 'sudo systemctl status timetable-backend'

# 重启后端
ssh tangent-server 'sudo systemctl restart timetable-backend'

# 查看后端日志
ssh tangent-server 'sudo journalctl -u timetable-backend -f'
```

Nginx 配置在宝塔 vhost 目录，修改后重载：
```bash
ssh tangent-server 'sudo nginx -t && sudo nginx -s reload'
```

---

## 4. 更新流程

### 4.1 后端更新
```bash
# 1. 本地打包（Java 17+，使用项目自带 Maven Wrapper）
cd backend && ./mvnw package -DskipTests

# 2. 上传 jar
scp backend/target/timetable-backend-1.0.0.jar tangent-server:/opt/timetable/backend/

# 3. 重启
ssh tangent-server 'sudo chown www:www /opt/timetable/backend/timetable-backend-1.0.0.jar && sudo systemctl restart timetable-backend'
```

### 4.2 前端更新
```bash
# 1. 本地构建
cd frontend && npm run build        # 产物在 dist/

# 2. 上传到临时目录（www 拥有目标目录，admin 无写权限，走 sudo）
ssh tangent-server 'rm -rf /tmp/tt-deploy && mkdir -p /tmp/tt-deploy/assets'
scp frontend/dist/index.html tangent-server:/tmp/tt-deploy/
scp frontend/dist/assets/* tangent-server:/tmp/tt-deploy/assets/

# 3. sudo 部署并设所有权
ssh tangent-server 'sudo rm -rf /www/wwwroot/todo.tangent0712.top/assets \
  && sudo cp -r /tmp/tt-deploy/assets /www/wwwroot/todo.tangent0712.top/ \
  && sudo cp /tmp/tt-deploy/index.html /www/wwwroot/todo.tangent0712.top/ \
  && sudo chown -R www:www /www/wwwroot/todo.tangent0712.top \
  && sudo rm -rf /tmp/tt-deploy'
```

> 前端构建产物文件名带 hash，静态资源配置了长缓存；部署后无需重启 nginx。

---

## 5. 数据库管理

```bash
# 登录 timetable 库（专用账号）
mysql -utimetable -p'<密码>' timetable

# 或 root（密码读环境变量 TANGENT_COMMON_PASSWORD）
mysql -uroot -p"$TANGENT_COMMON_PASSWORD" timetable
```

新增用户（凭据见本地维护的 `CREDENTIALS.md`，该文件不随仓库发布）：
```sql
INSERT IGNORE INTO api_key (api_key, label, avatar_url) VALUES ('tt_xxx', '用户名', '头像URL');
```

> 生产环境可创建 `application-prod.yml`（需自行维护，未随仓库发布）并设 `sql.init.mode=always`；schema.sql / data.sql 幂等，启动会自动建表并插入默认作息与 demo-key。已存在的表/数据用 `INSERT IGNORE` / `CREATE IF NOT EXISTS`，不会重复。

> ⚠️ **schema 变更需手动 ALTER**：`CREATE TABLE IF NOT EXISTS` 与 `CREATE ... IF NOT EXISTS`
> 只会新建**不存在的表**，**不会给已存在的表补列**。因此每次改 `schema.sql` 后，生产库需手动 ALTER。
> 历史手动 ALTER 示例：
> - `ALTER TABLE todo ADD COLUMN exam_id BIGINT NULL, ADD INDEX idx_todo_exam (exam_id);`
> - 移除考试课程关联：`ALTER TABLE exam DROP FOREIGN KEY exam_ibfk_2; ALTER TABLE exam DROP INDEX idx_exam_course; ALTER TABLE exam DROP COLUMN course_id;`
> 改 schema 前先 `SHOW CREATE TABLE <表名>` 确认外键/索引名再执行。

---

## 6. HTTPS 证书

- 证书：Let's Encrypt，路径 `/etc/letsencrypt/live/todo.tangent0712.top/`
- 由 certbot 的 `certbot-renew` systemd 定时器自动续期（webroot 方式，`/www/wwwroot/todo.tangent0712.top`）
- 手动续期验证：`ssh tangent-server 'sudo certbot renew --dry-run'`

---

## 7. 注意事项

1. **不要占用既有端口**：服务器已有 3000/3001/8080/8081/3456/12712 等服务，本项目用 8200，请勿改动。
2. **不要触碰其他数据库**：本应用只用 `timetable` 库；其余库（LLM_Cos / sacc_blog / tangent_main 等）属于其他项目。
3. **Nginx vhost 只新增** `todo.tangent0712.top.conf`，不要修改其他站点配置。
4. 前端文件所有权为 `www:www`，部署需用 sudo 移动（见 4.2）。
5. 完整凭据见 `CREDENTIALS.md`（敏感文件，勿提交到公开仓库）。
