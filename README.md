# 大切课程表（Tangent Timetable）

面向大学生的**课程表 + 待办事项**一站式日程管理工具。以 AI 降低课表录入与维护成本，
以实时倒计时、循环待办、考试管理覆盖整个学期的日程。

- 前端：Vite + React + TypeScript + Ant Design
- 后端：Spring Boot 3 + MyBatis-Plus + MySQL
- AI：DeepSeek（自然语言增删改查、HTML 课表解析、多轮对话、流式输出）

## 功能特性

- **课表管理**：多课表、周次与单双周、跨节课程、可视化 7×12 网格、冲突提示。
- **待办事项**：秒级 DDL 倒计时，支持分类与完成状态。
- **循环待办**：每日 / 每周 / 每月规则自动生成；支持用 JavaScript 编写
  `shouldTrigger(ctx)` 自定义规则（Rhino 沙箱隔离，五层安全防护）。
- **考试记录**：直接输入日期与起止时间，课表内以红色块展示，自动生成关联待办，
  考试结束后自动完成。
- **AI 助手**：粘贴教务 HTML 自动解析导入；用自然语言增删改课表、待办、考试、循环规则；
  多轮对话、SSE 流式输出；所有写操作以「提案卡片」形式逐条确认后执行，
  并有工作空间隔离与提案失效机制防止数据错乱。
- **体验**：深浅色主题、全局与课表字体缩放、响应式布局（移动端自适应）。
- **iPad 小组件（附加）**：用 WebView 组件加载 `/widget` 只读展示页，通过 RESTful 接口
  每 15 分钟刷新课程与待办。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17、Spring Boot 3.2、MyBatis-Plus 3.5、MySQL 8 |
| 认证 | `X-API-Key` 请求头 + 拦截器 |
| AI | DeepSeek API（JSON Mode + SSE 流式） |
| 脚本沙箱 | Mozilla Rhino 1.7.14（自定义循环规则） |
| 前端 | Vite 5、React 18、TypeScript 5、Ant Design 5 |

## 目录结构

```
Tangent-Timetable/
├── backend/                 # Spring Boot 后端
│   ├── mvnw / .mvn/         # Maven Wrapper（无需另装 Maven）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/timetable/
│       │   ├── config/ controller/ dto/ entity/ mapper/
│       │   ├── service/ service/impl/ interceptor/ exception/
│       │   └── TimetableApplication.java
│       └── resources/
│           ├── application.yml
│           └── db/schema.sql, db/data.sql
├── frontend/                # React 前端
│   └── src/{api,components,pages,store,utils,types}
├── docs/                    # 项目文档
│   ├── PRD.md               # 产品需求
│   ├── ARCHITECTURE.md      # 架构说明
│   ├── DEPLOYMENT.md        # 部署指南
│   ├── USER_MANUAL.md       # 用户手册
│   └── IPAD_WEBVIEW_WIDGET.md
├── README.md
└── LICENSE
```

## 快速开始

### 环境要求

JDK 17+、MySQL 8、Node.js 18+。Maven 由项目自带的 `./mvnw` 提供。

### 1. 启动后端

```bash
cd backend

# 数据库密码（默认回退为 root，可用环境变量覆盖）
export TANGENT_COMMON_PASSWORD=你的数据库密码

# 可选：配置后才启用 AI 功能
export DEEPSEEK_API_KEY=sk-xxx

./mvnw spring-boot:run
```

默认连接 `jdbc:mysql://localhost:3306/timetable`（不存在会自动建库），
首次启动自动执行 `db/schema.sql` 与 `db/data.sql`，写入演示账号 `demo-key-001`。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173，/api 已代理到 :8080
```

浏览器打开后，在登录页输入 API Key（默认演示账号 `demo-key-001`）即可进入。

## 配置

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `TANGENT_COMMON_PASSWORD` | MySQL 密码 | `root` |
| `DEEPSEEK_API_KEY` | DeepSeek 密钥；不配置则 AI 功能禁用 | 空 |
| `DEEPSEEK_BASE_URL` | DeepSeek 接口地址 | `https://api.deepseek.com` |
| `DEEPSEEK_MODEL` | 模型名 | `deepseek-v4-flash` |

更多示例见 `.env.example`。**请勿提交任何真实密钥。**

## API 概览

所有接口（除 `/api/auth/verify` 与只读的 `/api/widget/**`）需携带请求头
`X-API-Key: <your-key>`。统一响应格式：

```json
{ "code": 200, "message": "success", "data": {} }
```

| 分组 | 代表接口 |
|------|----------|
| 认证 | `GET /api/auth/verify` |
| 课表 | `GET/POST /api/schedules`、`GET /api/schedules/{id}/courses` |
| 课程 | `POST /api/schedules/{id}/courses`、`PUT/DELETE /api/courses/{id}` |
| 待办 | `GET/POST /api/todos`、`PUT /api/todos/{id}/toggle` |
| 循环待办 | `GET/POST /api/recurring-todos`、`POST /api/recurring-todos/{id}/trigger` |
| 考试 | `GET/POST /api/schedules/{id}/exams`、`PUT/DELETE /api/exams/{id}` |
| 作息 | `GET/PUT /api/period-config` |
| AI | `POST /api/ai/conversations/{id}/messages/stream`、`POST /api/ai/parse-html` |
| 小组件 | `GET /api/widget/overview/{key}?scheduleId=` |

完整接口清单与实现细节见 `docs/ARCHITECTURE.md` 与 `AGENTS.md`。

## 相关文档

- 产品需求：[docs/PRD.md](docs/PRD.md)
- 架构说明：[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- 部署指南：[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)
- 用户手册：[docs/USER_MANUAL.md](docs/USER_MANUAL.md)
- iPad 小组件：[docs/IPAD_WEBVIEW_WIDGET.md](docs/IPAD_WEBVIEW_WIDGET.md)

## License

[MIT](LICENSE) © 2026 Tangent0712
