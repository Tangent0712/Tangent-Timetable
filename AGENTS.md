# AGENTS.md — TimeTable 项目开发指南

> 面向接手的 AI Agent，快速了解项目全貌、约定和当前进度。

---

## 1. 项目概述

**大学生课程表+待办事项** 一站式日程管理工具，覆盖 Web / Android / macOS 三端。

- **Git 仓库**: `main` 分支
- **详细需求**: `docs/PRD.md` (v0.5)
- **Android 灵动岛技术指南**: `docs/SUPER_ISLAND_INTEGRATION_GUIDE.md`

---

## 2. 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL 8.x |
| 构建 | Maven (wrapper 未配置，用系统 mvn) |
| 认证 | `X-API-Key` Header 拦截器 |
| Web 前端 | Vite 5 + React 18 + TypeScript + Ant Design 5 *(已实现)* |
| Android | Kotlin + Jetpack Compose *(尚未实现)* |
| macOS | SwiftUI + WidgetKit *(尚未实现)* |
| AI | DeepSeek API (deepseek-chat, JSON Mode) *(已实现)* |

---

## 3. 项目结构

```
ToDoList- TimeTable/
├── .gitignore
├── docs/
│   ├── PRD.md                              # 产品需求文档
│   └── SUPER_ISLAND_INTEGRATION_GUIDE.md   # 小米超级岛技术指南
├── AGENTS.md                               # 本文件
├── backend/                                # Spring Boot 后端 (已实现)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/timetable/
│       │   ├── TimetableApplication.java      # 入口
│       │   ├── config/
│       │   │   ├── WebConfig.java             # CORS 配置
│       │   │   ├── WebMvcConfig.java          # 拦截器注册
│       │   │   └── DeepSeekProperties.java    # deepseek.api.* 配置绑定
│       │   ├── controller/
│       │   │   ├── AuthController.java        # /api/auth
│       │   │   ├── ScheduleController.java    # /api/schedules
│       │   │   ├── CourseController.java      # /api/courses, /api/schedules/{id}/courses
│       │   │   ├── TodoController.java        # /api/todos
│       │   │   ├── PeriodConfigController.java # /api/period-config
│       │   │   └── AiController.java          # /api/ai
│       │   ├── dto/
│       │   │   ├── ApiResponse.java           # 统一响应 {code, message, data}
│       │   │   ├── AuthVerifyResponse.java
│       │   │   ├── ScheduleRequest.java / CourseRequest.java
│       │   │   ├── TodoRequest.java / PeriodConfigRequest.java
│       │   │   ├── BatchCourseRequest.java
│       │   │   └── AiConversationRequest / AiMessageRequest / AiMessageResponse
│       │   │       / AiActionDto / AiParseHtmlRequest
│       │   ├── entity/
│       │   │   ├── ApiKey.java                # api_key 表
│       │   │   ├── Schedule.java              # schedule 表
│       │   │   ├── Course.java                # course 表
│       │   │   ├── Todo.java                  # todo 表
│       │   │   ├── PeriodConfig.java          # period_config 表
│       │   │   ├── AiConversation.java        # ai_conversation 表
│       │   │   └── AiMessage.java             # ai_message 表
│       │   ├── mapper/                        # MyBatis-Plus Mapper (7个)
│       │   ├── service/
│       │   │   ├── ApiKeyService / ScheduleService / CourseService
│       │   │   │   / TodoService / PeriodConfigService
│       │   │   ├── AiService.java             # AI 对话/执行/HTML解析
│       │   │   ├── DeepSeekClient.java        # JSON Mode 调用抽象
│       │   │   └── impl/
│       │   │       ├── AiServiceImpl.java     # 提案存储 + execute 落库
│       │   │       ├── AiPromptBuilder.java   # System Prompt (日期/周次/上下文注入)
│       │   │       └── DeepSeekClientImpl.java # java.net.http.HttpClient
│       │   ├── interceptor/
│       │   │   ├── ApiKeyInterceptor.java     # X-API-Key 认证
│       │   │   └── RequestContext.java        # ThreadLocal 上下文
│       │   └── exception/
│       │       ├── BusinessException.java
│       │       └── GlobalExceptionHandler.java
│       └── resources/
│           ├── application.yml
│           └── db/
│               ├── schema.sql                 # 建表 DDL
│               └── data.sql                   # 默认数据 (1个demo key + 12节作息)
└── frontend/                               # Vite + React + TS + antd (已实现)
    ├── package.json / tsconfig.json / vite.config.ts   # dev 代理 /api → :8080
    ├── index.html
    └── src/
        ├── main.tsx                        # ConfigProvider(zhCN) + Router + AppProvider
        ├── App.tsx                         # 未登录→LoginPage，已登录→AppLayout+路由
        ├── index.css                       # 课表网格/聊天气泡等自定义样式
        ├── types/index.ts                  # 所有后端 DTO 的 TS 类型
        ├── api/
        │   ├── client.ts                   # fetch 封装、X-API-Key、401 全局处理
        │   └── index.ts                    # authApi/scheduleApi/courseApi/todoApi
        │                                   #  /periodApi/aiApi
        ├── store/AppContext.tsx            # 全局状态 + 30s 轮询同步
        ├── utils/
        │   ├── schedule.ts                 # 周次换算、倒计时格式化、周次区间解析
        │   └── color.ts                    # 课程名 → 稳定配色
        ├── components/
        │   ├── AppLayout.tsx               # 顶栏导航 + 课表切换 + 周次显示 + 登出
        │   ├── TimetableGrid.tsx           # 7×12 网格，跨节 rowSpan，冲突选一+角标
        │   ├── CourseFormModal.tsx         # 课程增删改 + 周次快捷输入
        │   ├── ScheduleManagerModal.tsx    # 课表 CRUD
        │   ├── ImportHtmlModal.tsx         # 粘贴HTML→AI解析→预览勾选→批量导入
        │   └── AiActionCard.tsx            # AI 操作提案卡片 + 确认执行/取消
        └── pages/
            ├── LoginPage.tsx               # API Key 登录
            ├── TimetablePage.tsx           # 按周/全部视图、周次切换
            ├── TodosPage.tsx               # 待办列表 + 秒级倒计时
            ├── AiChatPage.tsx              # 多轮对话 + 会话列表
            └── SettingsPage.tsx            # 作息时间表配置 + 账号
```

---

## 4. 开发命令

### 后端

```bash
# 编译 (需先装好 Java 17+ 和 Maven)
cd backend
mvn compile

# 运行 (需先启动 MySQL)
mvn spring-boot:run

# 打包
mvn package -DskipTests
```

AI 功能需要设置环境变量后再启动：

```bash
export DEEPSEEK_API_KEY=sk-xxxx
mvn spring-boot:run
```

未设置时 `/api/ai/status` 返回 `enabled=false`，前端会禁用 AI 输入框和导入功能，其余功能正常。

### 前端

```bash
cd frontend
npm install          # 首次
npm run dev          # 开发服务器 http://localhost:5173，/api 代理到 :8080
npm run typecheck    # tsc --noEmit
npm run build        # tsc -b && vite build → dist/
```


---

## 5. 数据库

### 表结构

| 表 | 主键 | 说明 |
|----|------|------|
| `api_key` | api_key (VARCHAR 64) | API Key 账号，管理员预设 |
| `schedule` | id (BIGINT AUTO) | 课表，含 api_key 外键 |
| `period_config` | period_number (TINYINT) | 作息时间表，全局配置 |
| `course` | id (BIGINT AUTO) | 课程，含 schedule_id 外键，weeks 为 JSON |
| `todo` | id (BIGINT AUTO) | 待办，含 api_key 外键 |
| `ai_conversation` | id (BIGINT AUTO) | AI 对话，含 api_key + schedule_id |
| `ai_message` | id (BIGINT AUTO) | AI 消息，含 role/content（action_* 列已废弃，保留兼容） |
| `ai_action` | id (BIGINT AUTO) | AI 操作提案，一条消息可有多条；含 scope/status/data/fingerprint |

### MySQL 连接

**Host**: `localhost:3306` / **Database**: `timetable` / **User**: `root` / **Password**: `root`

启动时 `application.yml` 配置了 `spring.sql.init.mode=always`，会自动执行 `schema.sql` 和 `data.sql`。

### 默认数据

- API Key: `demo-key-001` (label: 默认用户)
- 12 节作息时间 (08:00–21:05, MORNING/AFTERNOON/EVENING)

---

## 6. API 接口

### 认证方式

所有接口（除 `/api/auth/verify`）需在 Header 中携带 `X-API-Key: <your-key>`。

### 接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/verify` | 验证 API Key，返回 `{valid, label}` |
| GET | `/api/schedules` | 获取当前用户所有课表 |
| POST | `/api/schedules` | 新建课表 |
| GET | `/api/schedules/{id}` | 获取课表详情（含课程列表） |
| PUT | `/api/schedules/{id}` | 修改课表 |
| DELETE | `/api/schedules/{id}` | 删除课表 |
| GET | `/api/schedules/{id}/courses` | 获取课表下所有课程 |
| POST | `/api/schedules/{id}/courses` | 添加单条课程 |
| POST | `/api/schedules/{id}/courses/batch` | 批量添加课程 |
| PUT | `/api/courses/{id}` | 修改课程 |
| DELETE | `/api/courses/{id}` | 删除课程 |
| GET | `/api/todos` | 获取待办列表 |
| POST | `/api/todos` | 创建待办 |
| PUT | `/api/todos/{id}` | 修改待办 |
| DELETE | `/api/todos/{id}` | 删除待办 |
| PUT | `/api/todos/{id}/toggle` | 切换待办完成状态 |
| GET | `/api/period-config` | 获取作息时间表 |
| PUT | `/api/period-config` | 修改作息时间表 |
| GET | `/api/ai/status` | AI 是否可用 `{enabled}` |
| GET | `/api/ai/conversations` | 对话列表 |
| POST | `/api/ai/conversations` | 创建对话 `{scheduleId}` |
| PUT | `/api/ai/conversations/{id}` | 重命名对话 `{title}` |
| DELETE | `/api/ai/conversations/{id}` | 删除对话 |
| GET | `/api/ai/conversations/{id}/messages` | 对话历史 |
| POST | `/api/ai/conversations/{id}/messages` | 发消息（非流式）→ `{text, actions[]}` |
| POST | `/api/ai/conversations/{id}/messages/stream` | **SSE 流式发消息**，事件 `thinking`/`text`/`planning`/`done`/`error` |
| POST | `/api/ai/conversations/{id}/actions/{actionId}/execute` | 确认执行单个提案 |
| POST | `/api/ai/conversations/{id}/actions/{actionId}/reject` | 取消单个提案 |
| POST | `/api/ai/parse-html?scheduleId=` | 解析教务 HTML → `{courses, note}` |

### 统一响应格式

```json
{ "code": 200, "message": "success", "data": {...} }
```

- 成功: `code=200`
- 参数校验失败: `code=400`
- 认证失败: `code=401`
- 业务错误 (如404): 各状态码
- 服务器错误: `code=500`

---

## 7. 代码约定

### 禁止使用 Lombok

**本项目不使用 Lombok**。原因是 JDK 26 环境下 Lombok 注解处理器不兼容。

所有 entity、dto、controller、service 必须**手动编写**：
- getter / setter
- 构造函数
- Logger (用 `LoggerFactory.getLogger` 而非 `@Slf4j`)

### 架构分层

```
Controller → Service (接口) → ServiceImpl → Mapper (MyBatis-Plus)
```

- Controller 通过 `RequestContext.getApiKey()` 获取当前用户
- Service 接收 `apiKey` 参数做数据隔离
- 异常统一抛出 `BusinessException(code, message)`，由 `GlobalExceptionHandler` 捕获

### 数据隔离

- Schedule / Todo 表通过 `api_key` 字段隔离用户
- Course 通过 `schedule_id` 关联到 Schedule，再间接隔离
- PeriodConfig 为全局配置，无数据隔离
- Service 层在查询/修改前校验所有权

---

## 8. 开发进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 后端核心 | 数据库建表、CRUD API、作息时间表 API | **已完成** |
| P1 AI 集成 | DeepSeek 解析HTML、自然语言CRUD、多轮对话 | **已完成** |
| P2 Web 前端 | React + Ant Design 课表视图、待办、AI 对话、导入 | **已完成** |
| P3 Android | 课表查看、待办查看、同步 | *未开始* |
| P4 Android 小组件 | Glance 4x6 Widget、DDL倒计时 | *未开始* |
| P5 Android 灵动岛 | FocusNotification + Shizuku + LiveUpdate | *未开始* |
| P6 Mac 小组件 | SwiftUI Notification Center Widget | *未开始* |

---

## 9. AI 实现说明

`application.yml` 中的 DeepSeek 配置：

```yaml
deepseek:
  api:
    key: ${DEEPSEEK_API_KEY:your-deepseek-api-key}
    base-url: https://api.deepseek.com
    model: deepseek-chat
```

- `DeepSeekClientImpl` 用 `java.net.http.HttpClient` 调 `/chat/completions`，
  开启 `response_format: {"type":"json_object"}`（JSON Mode）
- 未配置 key（或仍是占位值）时 `isConfigured()` 返回 false，`/api/ai/status` 返回
  `enabled=false`，业务调用抛 `503`
- `AiPromptBuilder.buildChatSystemPrompt` 注入：今天/明天日期与星期、当前第几周、
  近 4 周的「日期→第几周周几」对照表、作息时间表、该课表全部课程（带真实 id）、
  全部待办（带 id），并写明「下周一/每周一/周三」的语义约定
- AI 返回 `{text, actions[]}`，**一次可返回多个提案**，每个提案独立成一条 `ai_action`
  记录、独立确认执行，前端渲染成多张卡片
- 支持的 action.type：`CREATE_COURSE` / `UPDATE_COURSE` / `DELETE_COURSE` /
  `CREATE_TODO` / `UPDATE_TODO` / `DELETE_TODO` / `TOGGLE_TODO`
- 超出这 7 种能力（查天气、发邮件、改作息表等）或指令模糊时，Prompt 要求 AI
  直接简短说明做不到 / 追问，`actions` 返回 `[]`，不做长时间推理
- `max_tokens=16384` + `reasoning_effort=low` 限制推理预算，避免思考过长导致正文为空；
  若仍撞上 `finish_reason=length`，返回「AI 思考过长」提示而非笼统报错
- 执行时复用现有 `CourseService` / `TodoService`，因此所有权校验自动生效

### 流式输出（思考 → 输出 → 执行）

`POST /api/ai/conversations/{id}/messages/stream` 返回 SSE，事件顺序：

| 事件 | 时机 | 前端表现 |
|------|------|---------|
| `thinking` | 模型推理阶段（`reasoning_content` 增量） | 显示「思考中…」 |
| `text` | `text` 字段逐字生成 | 气泡内逐字上屏 |
| `planning` | `text` 结束、开始解析 actions | 显示「规划操作中…」 |
| `done` | 落库完成 | 用权威消息替换流式气泡 |
| `error` | 异常 | toast 报错并恢复输入内容 |

难点在于 JSON Mode 下流式拿到的是不完整 JSON，无法直接解析。
`JsonTextStreamExtractor` 用小状态机逐字扫描，只提取 `text` 字段的值，
因此要求 Prompt 把 `text` 放在 JSON 最前面。`text` 闭合引号出现即触发 `planning`。

### 提案失效机制（防数据混乱）

`ai_action.action_status` 四态：`PENDING` / `EXECUTED` / `REJECTED` / `STALE`。

**作用域隔离**：`scope` 为 `COURSE` 或 `TODO`，两者的指纹与失效**完全独立** ——
改一个待办不会让课表提案失效，反之亦然。

以下情况会把对应作用域的 `PENDING` 提案转为 `STALE`：

1. **产生新提案时** —— 仅让本次涉及的作用域内旧提案失效
2. **执行某条提案后** —— 同作用域、**不同消息**的提案失效；
   同一条消息内的兄弟提案会把指纹刷新到最新，**仍可继续逐个执行**
   （支持「一次规划多个操作，用户逐个确认」）
3. **用户手动改动** —— `CourseController` 传 `SCOPE_COURSE`，
   `TodoController` 传 `SCOPE_TODO`

兜底层：`ai_action.data_fingerprint` 存该作用域数据的 SHA-256，
`executeAction` 执行前重算比对，不一致则标记 STALE 并返回 409。

> 注意：MyBatis-Plus 的 `updateById` 会忽略 null 字段，
> 所以刷新兄弟提案指纹时必须写入重新计算的值，不能置 null。

### 修改前后对比

`enrichBeforeSnapshot` 会在 `UPDATE_* / DELETE_* / TOGGLE_TODO` 提案的
`data._before` 里写入修改前的完整记录快照，前端 `AiActionCard` 用它渲染
「修改前 / 修改后」差异表格（变化字段高亮）。`applyAction` 只读取已知字段，
`_before` 不参与执行。

### 对话命名

默认标题为 `nextDefaultTitle()` 生成的 `yyyy-MM-dd #NN`（NN 为当天第几个对话，
两位、从 01 开始）。用户可在会话列表点铅笔图标重命名，走 `PUT /api/ai/conversations/{id}`。

---

## 10. 注意事项

1. **多用户设计**: 每个 API Key 独立数据空间，Key 由管理员在数据库 `api_key` 表预设
2. **Course.weeks**: MySQL JSON 类型，MyBatis-Plus 用 `JacksonTypeHandler` 映射为 `List<Integer>`
3. **CORS**: `WebConfig` 中对 `/api/**` 放开了所有来源
4. **事务**: `BatchCourseRequest` 批量添加课程标注了 `@Transactional`
5. **Schedule GET /{id}**: 返回 `{schedule: {...}, courses: [...]}` 组合结构
