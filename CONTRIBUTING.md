# 贡献指南

感谢你愿意为「大切课程表」做出贡献。本文档说明如何搭建开发环境、提交代码与约定。

## 开发环境

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 使用项目自带的 `./mvnw`（无需另装） |
| MySQL | 8.x |
| Node.js | 18+ |

## 本地启动

### 后端

```bash
cd backend
export TANGENT_COMMON_PASSWORD=你的数据库密码
export DEEPSEEK_API_KEY=sk-xxx        # 可选，不配则 AI 功能禁用

./mvnw spring-boot:run                # 默认读取 application.yml，端口 8080
# 或使用本地 profile（application-local.yml，含本地专用配置且已被 git 忽略）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

首次启动会自动执行 `db/schema.sql` 建表并写入默认数据（含演示账号 `demo-key-001`）。

### 前端

```bash
cd frontend
npm install
npm run dev                           # http://localhost:5173，/api 代理到 :8080
```

## 提交前检查

```bash
# 后端
cd backend && ./mvnw -q compile

# 前端
cd frontend && npm run typecheck && npm run build
```

## 代码约定

- **不使用 Lombok**：所有 entity / DTO / service 手写 getter、setter 与构造函数。
- 后端分层：`Controller → Service(接口) → ServiceImpl → Mapper`。
- 业务异常统一抛 `BusinessException`。
- 前端使用函数组件 + TypeScript，遵循现有目录与命名习惯。
- 不要提交任何密钥、密码或本地配置文件。

## 提交信息

采用简洁的祈使句，可用前缀：`feat:` / `fix:` / `docs:` / `refactor:` / `chore:`。例如：

```
fix(backend): 修正循环待办在二月末的触发日期
```

## Pull Request

1. 从 `main` 切出功能分支。
2. 保证上述检查命令通过。
3. 在 PR 描述中说明改动动机、实现方式与验证步骤。
