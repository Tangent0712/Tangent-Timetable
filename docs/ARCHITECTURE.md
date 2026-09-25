# 架构说明

> 更新日期：2026-09-26

本文档说明「大切课程表」的整体架构、后端分层、数据模型与关键机制，便于二次开发与维护。

---

## 1. 总体架构

前后端分离的 Web 应用：

```
浏览器 (Vite + React SPA)
   │  REST /api/**（X-API-Key 请求头）
   ▼
Spring Boot 后端  ──►  MySQL
   │
   └──►  DeepSeek API（AI 解析与对话）
```

- 后端为唯一数据源，前端通过 REST 接口读写，并每 30 秒轮询同步。
- 认证采用 `X-API-Key` 请求头 + 拦截器，Key 即账号，用于数据隔离。
- AI 能力接入 DeepSeek；未配置密钥时自动禁用 AI 功能，其余功能不受影响。

---

## 2. 后端分层

```
Controller → Service(接口) → ServiceImpl → Mapper (MyBatis-Plus) → MySQL
```

- `controller/`：暴露 REST 接口，从 `RequestContext.getApiKey()` 取当前用户。
- `service/` + `service/impl/`：业务逻辑，接收 `apiKey` 参数做所有权校验。
- `mapper/`：MyBatis-Plus Mapper。
- `entity/` / `dto/`：数据库实体与请求/响应对象（**不使用 Lombok**，手写 getter/setter）。
- `interceptor/`：`ApiKeyInterceptor` 校验请求头并写入 `RequestContext`。
- `exception/`：`BusinessException` + `GlobalExceptionHandler` 统一处理。
- `config/`：Web/CORS、Jackson、DeepSeek 配置，以及循环待办、考试待办定时器。

### 关键模块

| 模块 | 说明 |
|------|------|
| 课表 / 课程 | `ScheduleService`、`CourseService`，课程 `weeks` 为 JSON 数组 |
| 待办 | `TodoService`，支持循环待办与考试关联待办 |
| 循环待办 | `RecurringTodoService` + `RecurringTodoScheduler`（每 60s 扫描）；自定义规则由 `RecurringScriptEvaluator`（Rhino 沙箱）执行 |
| 考试 | `ExamService`，创建/修改/删除考试时同步关联待办；`ExamTodoScheduler` 自动完成已结束考试的待办 |
| AI | `AiService` + `AiPromptBuilder` + `DeepSeekClient`，提案存储与执行、工作空间保护 |
| 作息时间表 | `PeriodConfigService`，全局配置 |
| 小组件 | `WidgetService`，只读聚合接口 |

---

## 3. 数据模型

| 表 | 主键 | 说明 |
|----|------|------|
| `api_key` | api_key | API Key 账号（label/avatar_url） |
| `schedule` | id | 课表，含 `api_key` 外键 |
| `period_config` | period_number | 作息时间表（全局） |
| `course` | id | 课程，含 `schedule_id` 外键，`weeks` 为 JSON |
| `exam` | id | 考试，含 `schedule_id` 外键（不关联课程） |
| `todo` | id | 待办，含 `api_key` 外键；`recurring_id` / `exam_id` |
| `recurring_todo` | id | 循环待办规则 |
| `ai_conversation` | id | AI 对话，含 `api_key` + `schedule_id` |
| `ai_message` | id | AI 消息（role/content） |
| `ai_action` | id | AI 操作提案（scope/status/data/fingerprint） |

建表脚本见 `backend/src/main/resources/db/schema.sql`。

### 数据隔离

- `schedule` / `todo` 通过 `api_key` 隔离。
- `course` / `exam` 通过 `schedule_id` 间接隔离。
- `period_config` 为全局配置。
- Service 层在查询/修改前校验所有权。

---

## 4. 认证与安全

- 除 `/api/auth/verify` 与只读的 `/api/widget/**` 外，所有接口需 `X-API-Key`。
- 统一响应：`{ code, message, data }`。
- AI 执行层复用各业务 Service，所有权校验自动生效。
- 自定义循环脚本运行于 Rhino 沙箱，具备 ClassShutter、指令计数、栈深限制、堆哨兵与脚本长度上限五层防护。

---

## 5. AI 模块

- `DeepSeekClient` 以 JSON Mode 调用 `/chat/completions`，流式接口为 SSE。
- `AiPromptBuilder` 注入当前日期/周次、作息表、课表全部课程/考试/待办（含真实 id）。
- AI 返回 `{ text, actions[] }`，每条数据库操作拆成独立 `ai_action`，前端逐条确认执行。
- 提案状态 `PENDING / EXECUTED / REJECTED / STALE`，按 `COURSE` / `TODO` 作用域独立失效，另有 SHA-256 数据指纹兜底。
- 每个对话绑定一个课表作为工作空间（`ai_conversation.schedule_id`），防止跨课表操作。

---

## 6. 前端架构

- `main.tsx` 组合 FontProvider / ThemeProvider / Router / AppProvider。
- `store/AppContext.tsx` 全局状态 + 30s 轮询；`store/FontContext.tsx` 字体缩放。
- `api/`：fetch 封装、各业务 API、SSE 流解析。
- `components/` 与 `pages/`：课表网格、课程/考试/课表管理弹窗、AI 对话、待办、设置、用户手册。
- `utils/`：周次换算、倒计时格式化、配色。

---

## 7. 部署

生产为 Nginx（静态前端 + `/api` 反向代理） + Spring Boot（systemd） + MySQL。
详见 `docs/DEPLOYMENT.md`。
