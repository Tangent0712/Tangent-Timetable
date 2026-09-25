# 部署指南

> 更新日期：2026-09-26

「大切课程表」是前后端分离应用，推荐的部署形态为：**Nginx 托管前端静态文件 + 反向代理 `/api`，
Spring Boot 以 systemd 常驻，MySQL 存数据**。本文档给出通用部署与运维流程，示例中的
`<DOMAIN>`、`<SERVER_HOST>`、`<APP_DIR>`、`<WEB_ROOT>` 请按实际环境替换。

---

## 1. 架构总览

```
浏览器
  │  https://<DOMAIN>
  ▼
Nginx ── 静态文件 <WEB_ROOT>/（React 构建产物）
  │  location /api/ 反向代理
  ▼
Spring Boot 127.0.0.1:8080（systemd 托管）
  │
  ▼
MySQL 数据库 timetable（建议独立库 + 专用账号）
```

- 前端：Vite + React + TS + Ant Design，构建产物为纯静态文件。
- 后端：Spring Boot 3，端口由 `server.port` 配置（默认 8080）。
- 数据库：建议使用独立库与专用账号，避免影响服务器上其他应用。

---

## 2. 后端部署

### 2.1 打包

```bash
cd backend
./mvnw package -DskipTests        # 产物：target/timetable-backend-1.0.0.jar
```

### 2.2 生产配置

复制 `backend/src/main/resources/application.yml` 为 `application-prod.yml`（该文件已被 `.gitignore` 忽略，
请勿提交），按需设置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/timetable?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: timetable
    password: ${TANGENT_COMMON_PASSWORD}
  sql:
    init:
      mode: always          # schema.sql / data.sql 幂等，启动自动建表与种子数据
server:
  port: 8080
```

### 2.3 systemd 服务

`/etc/systemd/system/timetable-backend.service`：

```ini
[Unit]
Description=Tangent Timetable Backend
After=network.target mysql.service

[Service]
WorkingDirectory=<APP_DIR>
EnvironmentFile=<APP_DIR>/.env
ExecStart=/usr/bin/java -jar <APP_DIR>/timetable-backend-1.0.0.jar --spring.profiles.active=prod
Restart=on-failure
User=www-data

[Install]
WantedBy=multi-user.target
```

启用与运维：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now timetable-backend
sudo systemctl status timetable-backend
sudo journalctl -u timetable-backend -f
```

---

## 3. 前端部署

### 3.1 构建

```bash
cd frontend
npm ci
npm run build                     # 产物在 dist/
```

可通过 `VITE_API_BASE` 指定 API 基地址（默认 `/api`，经 Nginx 代理即可，无需修改）。

### 3.2 上传静态文件

```bash
# 上传到临时目录，再移动到站点目录（需要相应权限）
scp -r frontend/dist/* <SERVER_HOST>:/tmp/tt-deploy/
ssh <SERVER_HOST> 'sudo mkdir -p <WEB_ROOT> && sudo cp -r /tmp/tt-deploy/* <WEB_ROOT>/ \
  && sudo chown -R www-data:www-data <WEB_ROOT> && rm -rf /tmp/tt-deploy'
```

> 构建产物文件名带内容 hash，静态资源可配置长缓存；部署后一般无需重启 Nginx。

### 3.3 Nginx 示例

```nginx
server {
    listen 443 ssl http2;
    server_name <DOMAIN>;

    root <WEB_ROOT>;
    index index.html;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源长缓存
    location /assets/ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 反向代理后端
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 4. 数据库

```bash
# 创建独立库与账号（示例）
mysql -uroot -p <<'SQL'
CREATE DATABASE IF NOT EXISTS timetable DEFAULT CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'timetable'@'localhost' IDENTIFIED BY '<PASSWORD>';
GRANT ALL PRIVILEGES ON timetable.* TO 'timetable'@'localhost';
FLUSH PRIVILEGES;
SQL
```

新增登录用户（`api_key` 即账号）：

```sql
INSERT IGNORE INTO api_key (api_key, label, avatar_url)
VALUES ('tt_xxx', '用户名', '头像URL');
```

> **schema 变更需手动 ALTER**：`CREATE TABLE IF NOT EXISTS` 只会创建不存在的表，
> **不会给已存在的表补列**。因此每次修改 `schema.sql` 后，已有数据库需手动执行 `ALTER`。
> 改前先 `SHOW CREATE TABLE <表名>` 确认外键/索引名。示例：
> ```sql
> ALTER TABLE todo ADD COLUMN exam_id BIGINT NULL, ADD INDEX idx_todo_exam (exam_id);
> ```

---

## 5. HTTPS

使用 Let's Encrypt（certbot）签发证书，并配置自动续期：

```bash
sudo certbot --nginx -d <DOMAIN>          # 首次签发
sudo certbot renew --dry-run              # 验证续期
```

---

## 6. 更新流程

```bash
# 1. 本地构建
cd backend && ./mvnw package -DskipTests
cd ../frontend && npm ci && npm run build

# 2. 上传后端 jar 并重启
scp backend/target/timetable-backend-1.0.0.jar <SERVER_HOST>:<APP_DIR>/
ssh <SERVER_HOST> 'sudo systemctl restart timetable-backend'

# 3. 上传前端静态文件（见 3.2）
```

---

## 7. 注意事项

1. **端口冲突**：部署前确认 `server.port` 未被占用。
2. **数据库隔离**：建议为应用单独建库，不触碰服务器上的其他数据库。
3. **Nginx**：只新增本应用的 server 配置，不要修改其他站点。
4. **密钥管理**：数据库密码、DeepSeek Key 通过环境变量或 `application-prod.yml` 注入，
   **不要提交到仓库**。
