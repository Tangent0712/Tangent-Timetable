# AGENTS.md — TimeTable 项目开发指南

> 面向接手的 AI Agent，快速了解项目全貌、约定和当前进度。

---

## 1. 项目概述

**大学生课程表+待办事项** 一站式日程管理工具，覆盖 Web / Android / macOS 三端。

- **Git 仓库**: `main` 分支
- **详细需求**: `docs/PRD.md` (v0.5)
- **Android 灵动岛技术指南**: `docs/SUPER_ISLAND_INTEGRATION_GUIDE.md`
- **生产环境**: https://todo.tangent0712.top （已上线，部署见 `docs/DEPLOYMENT.md`）
- **账号与密钥**: `docs/CREDENTIALS.md` ⚠️ 含敏感信息，勿提交到公开仓库

---

## 2. 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL 8.x |
| 构建 | Maven (wrapper 未配置，用系统 mvn) |
| 认证 | `X-API-Key` Header 拦截器 |
| Web 前端 | Vite 5 + React 18 + TypeScript + Ant Design 5 *(已实现)* |
| Android | Kotlin 2.2 + Jetpack Compose + Glance *(P3/P4/P5 已实现，见下)* |
| macOS | SwiftUI + WidgetKit *(尚未实现)* |
| AI | DeepSeek API (deepseek-v4-flash, JSON Mode + 流式) *(已实现)* |
| 脚本沙箱 | Mozilla Rhino 1.7.14（自定义循环规则，ClassShutter 隔离） |

---

## 3. 项目结构

```
ToDoList- TimeTable/
├── .gitignore
├── docs/
│   ├── PRD.md                              # 产品需求文档
│   ├── SUPER_ISLAND_INTEGRATION_GUIDE.md   # 小米超级岛技术指南
│   ├── DEPLOYMENT.md                       # 生产部署与运维指南
│   └── CREDENTIALS.md                      # ⚠️ 账号/密钥/密码（勿公开）
├── AGENTS.md                               # 本文件
├── backend/                                # Spring Boot 后端 (已实现)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/timetable/
│       │   ├── TimetableApplication.java      # 入口
│       │   ├── config/
│       │   │   ├── WebConfig.java             # CORS 配置
│       │   │   ├── WebMvcConfig.java          # 拦截器注册
│       │   │   ├── DeepSeekProperties.java    # deepseek.api.* 配置绑定
│       │   │   ├── JacksonConfig.java         # 宽松 LocalDateTime/LocalTime 反序列化
│       │   │   ├── RecurringTodoScheduler.java # 循环待办定时扫描 (60s)
│       │   │   └── ExamTodoScheduler.java     # 考试结束自动完成关联待办 (60s)
│       │   ├── controller/
│       │   │   ├── AuthController.java        # /api/auth
│       │   │   ├── ScheduleController.java    # /api/schedules
│       │   │   ├── CourseController.java      # /api/courses, /api/schedules/{id}/courses
│       │   │   ├── TodoController.java        # /api/todos
│       │   │   ├── ExamController.java        # /api/exams, /api/schedules/{id}/exams
│       │   │   ├── PeriodConfigController.java # /api/period-config
│       │   │   └── AiController.java          # /api/ai
│       │   ├── dto/
│       │   │   ├── ApiResponse.java           # 统一响应 {code, message, data}
│       │   │   ├── AuthVerifyResponse.java
│       │   │   ├── ScheduleRequest.java / CourseRequest.java / ExamRequest.java
│       │   │   ├── TodoRequest.java / PeriodConfigRequest.java
│       │   │   ├── BatchCourseRequest.java
│       │   │   └── AiConversationRequest / AiMessageRequest / AiMessageResponse
│       │   │       / AiActionDto / AiParseHtmlRequest
│       │   ├── entity/
│       │   │   ├── ApiKey.java                # api_key 表
│       │   │   ├── Schedule.java              # schedule 表
│       │   │   ├── Course.java                # course 表
│       │   │   ├── Exam.java                  # exam 表（schedule_id 归属，不关联课程）
│       │   │   ├── Todo.java                  # todo 表（含 recurring_id / exam_id）
│       │   │   ├── PeriodConfig.java          # period_config 表
│       │   │   ├── AiConversation.java        # ai_conversation 表
│       │   │   └── AiMessage.java             # ai_message 表
│       │   ├── mapper/                        # MyBatis-Plus Mapper (8个，含 ExamMapper)
│       │   ├── service/
│       │   │   ├── ApiKeyService / ScheduleService / CourseService / ExamService
│       │   │   │   / TodoService / PeriodConfigService
│       │   │   ├── AiService.java             # AI 对话/执行/HTML解析
│       │   │   ├── DeepSeekClient.java        # JSON Mode 调用抽象
│       │   │   └── impl/
│       │   │       ├── AiServiceImpl.java     # 提案存储 + execute 落库 + 工作空间保护
│       │   │       ├── AiPromptBuilder.java   # System Prompt (日期/周次/上下文注入)
│       │   │       ├── JsonTextStreamExtractor.java # 流式增量提取 text 字段
│       │   │       ├── ExamServiceImpl.java   # 考试 CRUD + 自动关联待办
│       │   │       ├── RecurringScheduleCalculator.java # 触发时间推算
│       │   │       ├── RecurringScriptEvaluator.java   # Rhino 沙箱 (安全关键)
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
        ├── main.tsx                        # FontProvider + ConfigProvider(zhCN) + Router + AppProvider
        ├── App.tsx                         # 未登录→LoginPage，已登录→AppLayout+路由
        ├── index.css                       # 课表网格/聊天气泡/响应式/字体变量等自定义样式
        ├── types/index.ts                  # 所有后端 DTO 的 TS 类型
        ├── api/
        │   ├── client.ts                   # fetch 封装、X-API-Key、401 全局处理
        │   └── index.ts                    # authApi/scheduleApi/courseApi/todoApi/examApi
        │                                   #  /periodApi/aiApi
        ├── store/
        │   ├── AppContext.tsx              # 全局状态 + 30s 轮询同步
        │   └── FontContext.tsx             # 字体大小设置（localStorage + CSS 变量）
        ├── utils/
        │   ├── schedule.ts                 # 周次换算、倒计时格式化、周次区间解析
        │   └── color.ts                    # 课程名 → 稳定配色
        ├── components/
        │   ├── AppLayout.tsx               # 顶栏 + 侧边栏导航（窄屏变抽屉）+ 登出
        │   ├── TimetableGrid.tsx           # 7×12 网格，跨节 rowSpan，冲突角标，考试红色块
        │   ├── CourseListView.tsx          # 全部课程（卡片式，非表格）
        │   ├── CourseFormModal.tsx         # 课程增删改 + 周次快捷输入
        │   ├── ExamManagerModal.tsx        # 考试管理（增删改，不含课程关联）
        │   ├── ScheduleManagerModal.tsx    # 课表 CRUD
        │   ├── ImportHtmlModal.tsx         # 粘贴HTML→AI解析→预览勾选→批量导入
        │   └── AiActionCard.tsx            # AI 操作提案卡片 + 确认执行/取消
        └── pages/
            ├── LoginPage.tsx               # API Key 登录
            ├── TimetablePage.tsx           # 按周/全部视图、周次切换、考试管理
            ├── TodosPage.tsx               # 待办列表 + 秒级倒计时 + 循环任务
            ├── AiChatPage.tsx              # 多轮对话 + 会话列表（工作空间保护）
            ├── UserManualPage.tsx          # 用户手册（内置页）
            └── SettingsPage.tsx            # 作息时间表(只读限制) + 字体大小 + 账号
└── android/                               # Android 客户端 (Kotlin + Compose，已实现 P3/P4/P5)
    ├── settings.gradle.kts / build.gradle.kts / gradle.properties
    ├── gradlew (Wrapper 8.13)
    ├── hidden-api/                        # android.net.IConnectivityManager 编译桩 (Shizuku)
    └── app/
        ├── build.gradle.kts               # minSdk 27, targetSdk 36
        └── src/main/
            ├── AndroidManifest.xml        # 前台服务 + Glance Widget + Shizuku Provider
            ├── java/com/timetable/android/
            │   ├── MainActivity.kt        # 登录→AppScaffold 导航 + 通知权限申请
            │   ├── TimetableApp.kt
            │   ├── data/                  # Models/Network/Retrofit API/Settings/Repository
            │   │   ├── Models.kt / TimetableApi.kt / Network.kt / Settings.kt
            │   │   ├── TimetableRepository.kt
            │   │   └── ScheduleEngine.kt  # 周次/当天课程/上课进度状态机
            │   ├── util/ScheduleCalc.kt   # 倒计时/周次换算 (移植自前端)
            │   ├── ui/                    # Compose: AppViewModel/AppScaffold/Theme + screen/
            │   │   ├── AppViewModel.kt    # 登录/登出/同步 + 启停提醒服务
            │   │   ├── AppScaffold.kt     # 底部导航 (课表/待办/设置)
            │   │   └── screen/            # Login/Timetable/Todos/Settings 页
            │   ├── widget/                # Glance 4x6 Widget + WidgetFetcher
            │   │   ├── ScheduleWidget.kt  # 当天课程最近两条 + 待办最近三条 + 倒计时
            │   │   └── WidgetFetcher.kt
            │   └── reminder/              # 上课/考试提醒
            │       ├── ReminderEngine.kt  # 课前/上课中/下课/考试 状态机
            │       ├── ScheduleReminderService.kt # 前台服务 30s 轮询
            │       ├── SuperIslandNotifier.kt     # focus-api + Shizuku XMSF 绕过
            │       ├── LiveUpdateNotifier.kt      # 标准通知降级
            │       ├── ShizukuHelper.kt   # Shizuku 权限 + Binder 封装
            │       └── Notifier.kt        # 通知渠道/构建
            └── res/                       # 图标/小组件布局/主题
```

---

## 4. 开发命令

### 后端

```bash
# 编译 (需先装好 Java 17+ 和 Maven)
cd backend
mvn compile

# 本地运行（推荐）：加载 application-local.yml（含本地 DB 密码 + DeepSeek key）
mvn spring-boot:run -Dspring-boot.run.profiles=local

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

### Android

```bash
cd android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew assembleDebug          # 构建 debug APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # 运行 JVM 单元测试 (ScheduleEngine)
./gradlew installDebug           # 安装到已连接设备/模拟器
```
> 依赖走国内镜像（Aliyun）替代被墙的 Maven Central；如遇 403 需检查网络镜像配置。
> 首次构建会自动用 Gradle 8.13 Wrapper 下载依赖。


---

## 5. 数据库

### 表结构

| 表 | 主键 | 说明 |
|----|------|------|
| `api_key` | api_key (VARCHAR 64) | API Key 账号，管理员预设 |
| `schedule` | id (BIGINT AUTO) | 课表，含 api_key 外键 |
| `period_config` | period_number (TINYINT) | 作息时间表，全局配置 |
| `course` | id (BIGINT AUTO) | 课程，含 schedule_id 外键，weeks 为 JSON |
| `exam` | id (BIGINT AUTO) | 考试，含 schedule_id 外键（不关联课程）；直接输入日期/起止时间 |
| `todo` | id (BIGINT AUTO) | 待办，含 api_key 外键；recurring_id 指循环规则，exam_id 指考试 |
| `ai_conversation` | id (BIGINT AUTO) | AI 对话，含 api_key + schedule_id |
| `ai_message` | id (BIGINT AUTO) | AI 消息，含 role/content（action_* 列已废弃，保留兼容） |
| `ai_action` | id (BIGINT AUTO) | AI 操作提案，一条消息可有多条；含 scope/status/data/fingerprint |
| `recurring_todo` | id (BIGINT AUTO) | 循环待办规则，含 frequency/触发时刻/截止偏移/自定义脚本 |

### MySQL 连接

**Host**: `localhost:3306` / **Database**: `timetable`
- 本地开发用 `application-local.yml`（datasource 密码 `<REDACTED_PASSWORD>`，且 `sql.init.mode=never`，
  故本地 schema 变更需**手动 ALTER**）
- 生产用 `application-prod.yml`（`sql.init.mode=always`，启动自动建表）

> 注意：本地 profile 是 `sql.init.mode=never`，改 schema.sql 后本地表结构不会自动更新，
> 需手动 `ALTER TABLE`。`application.yml` 默认 `root/root`，本地实际密码见 `application-local.yml`。

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
| GET | `/api/schedules/{id}/exams` | 获取课表下所有考试 |
| POST | `/api/schedules/{id}/exams` | 新增考试（body：name/location/examDate/startTime/endTime） |
| PUT | `/api/exams/{id}` | 修改考试 |
| DELETE | `/api/exams/{id}` | 删除考试 |
| GET | `/api/recurring-todos` | 循环待办规则列表 |
| POST | `/api/recurring-todos` | 新建循环规则 |
| PUT | `/api/recurring-todos/{id}` | 修改循环规则 |
| DELETE | `/api/recurring-todos/{id}` | 删除循环规则（已生成待办保留） |
| PUT | `/api/recurring-todos/{id}/toggle` | 启用/停用循环规则 |
| POST | `/api/recurring-todos/{id}/trigger` | 立即触发生成一条待办 |
| GET | `/api/recurring-todos/script-template` | 自定义脚本骨架 + 可用 ctx 字段 |
| POST | `/api/recurring-todos/test-script` | 试运行脚本 → `{valid, triggeredNow, error}` |
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

> 最后更新：2026-08-08

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 后端核心 | 数据库建表、CRUD API、作息时间表 API | **已完成** |
| P1 AI 集成 | DeepSeek 解析HTML、自然语言CRUD、多轮对话、流式输出 | **已完成** |
| P2 Web 前端 | React + Ant Design 课表视图、待办、AI 对话、导入 | **已完成（已上线 https://todo.tangent0712.top）** |
| P2.5 循环待办 | 每日/每周/每月规则、自动生成、AI 增删改查 | **已完成** |
| P2.6 自定义循环规则 | Rhino 沙箱脚本引擎 | **已完成** |
| P2.7 考试记录 | 直接输入起止时间、课表红色块展示、独立考试、自动关联待办、AI 增删改查 | **已完成** |
| P2.8 前端体验 | 响应式窄屏布局、字体大小设置、用户手册页、作息时间表只读权限 | **已完成** |
| P3 Android | 课表查看、待办查看、同步 | **已完成** |
| P4 Android 小组件 | Glance 4x6 Widget、DDL倒计时 | **已完成** |
| P5 Android 灵动岛 | FocusNotification + Shizuku + LiveUpdate | **已完成** |
| P6 Mac 小组件 | SwiftUI Notification Center Widget | *未开始* |

> **生产部署已完成**：站点 https://todo.tangent0712.top，后端跑在 <REDACTED_SERVER_IP>:8200。
> 部署与运维见 `docs/DEPLOYMENT.md`，全部账号/密钥见 `docs/CREDENTIALS.md`（敏感，勿提交）。
>
> **Android 客户端**：`android/`，Gradle 8.13 Wrapper + Kotlin 2.2 + AGP 8.7.3，
> 依赖国内镜像（Aliyun）替代被墙的 Maven Central。构建：`cd android && ./gradlew assembleDebug`。

### 待开发事项（TODO）

#### 1. 已知问题 / 可改进

- [ ] 前端打包体积 1.3MB（gzip 420KB），未做代码分割，
      可用 `manualChunks` 拆分 antd
- [ ] `MAX_CATCH_UP=5` 是硬编码，长期停机后会丢失更早的循环期次
- [ ] AI 幻觉无法根除，只能靠 Prompt 约束；
      执行层有所有权校验兜底，编造 id 只会 404
- [ ] 循环待办目前无「跳过本期」功能，只能删除生成的待办

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
  全部考试（带 id）、全部待办（带 id），
  并写明「下周一/每周一/周三」的语义约定
- AI 返回 `{text, actions[]}`，**一次可返回多个提案**，每条**数据库基本操作**独立成一条
  `ai_action` 记录、独立渲染成一张卡片，各自有独立的「确认执行/取消」按钮；
  **不做一键全部执行**（删除 7 条记录就是 7 张独立的「删除」卡片，逐条确认）
- 若 AI 把多条记录塞进同一个 action（如 `{"exams":[e1,e2,e3]}` 或 `{"courseIds":[1,2,3]}`），
  后端 `splitToSingleActions` 会按数组字段（courses/courseIds/todos/todoIds/rules/
  recurringIds/exams/examIds）**逐条拆成独立 ai_action**，保证一条操作一张卡、可单独确认执行
- 支持的 action.type：`CREATE_COURSE` / `UPDATE_COURSE` / `DELETE_COURSE` /
  `CREATE_TODO` / `UPDATE_TODO` / `DELETE_TODO` / `TOGGLE_TODO` /
  `CREATE_RECURRING` / `UPDATE_RECURRING` / `DELETE_RECURRING` / `TOGGLE_RECURRING` /
  `CREATE_EXAM` / `UPDATE_EXAM` / `DELETE_EXAM`
- 考试 action 的 `data`：`{"exams":[{"name":"课程名+考试类型",`
  `"location":...,"examDate":"yyyy-MM-dd","startTime":"HH:mm","endTime":"HH:mm"}]}`；
  考试不关联课程，直接用名称/类型命名（如「数据结构期末考试」「CET-6」）
- 超出这些能力（查天气、发邮件、改作息表等）或指令模糊时，Prompt 要求 AI
  直接简短说明做不到 / 追问，`actions` 返回 `[]`，不做长时间推理
- `max_tokens=16384` + `reasoning_effort=low` 限制推理预算，避免思考过长导致正文为空；
  若仍撞上 `finish_reason=length`，返回「AI 思考过长」提示而非笼统报错
- 执行时复用现有 `CourseService` / `TodoService` / `ExamService`，
  因此所有权校验自动生效

### 工作空间保护

每个 AI 对话绑定一个课表（`ai_conversation.schedule_id`）作为**工作空间**。
- 发消息时若请求的 `scheduleId` 与对话绑定课表不一致，后端抛 `400`
  （提示属于哪个课表，需切换或新建），防止串到其它课表的数据
- 前端 `AiChatPage` 计算 `workspaceMismatch`，不一致时**锁定**输入框/发送/执行/取消，
  顶部标签显示对话所属工作空间名称（不一致时红色 ⚠ 提示）
- 执行/取消提案走对话的 `scheduleId`（`executeAction` 用 `conversation.getScheduleId()`），
  天然限定在对话自己的工作空间内

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

### 历史上下文注入（易踩坑）

assistant 历史必须还原成「模型当初本该输出的合法 JSON」
（`{"text":..., "actions":[...]}`），提案的执行状态另发一条 `user` 角色的
`[系统反馈]` 消息。

> 曾经把提案拼成 `[提案: TYPE 状态: X 数据: {...}]` 塞进 assistant 消息，
> 结果模型模仿该格式、不再输出 JSON，导致解析失败。
> `parseJson` 现在也有兜底：先截取最外层 `{...}`，仍失败则降级为纯文本回复，
> 不中断对话。

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

## 10. 循环待办

规则存在 `recurring_todo`，到点由后端自动生成真实 `todo` 记录
（`todo.recurring_id` 指回规则）。

| 字段 | 说明 |
|------|------|
| `frequency` | `DAILY` / `WEEKLY` / `MONTHLY` |
| `day_of_week` | WEEKLY 时必填，1=周一 … 7=周日 |
| `day_of_month` | MONTHLY 时必填，1-31；当月无该日则顺延到最后一天（31 号遇 2 月 → 28/29） |
| `trigger_time` | 每期触发时刻 |
| `ddl_offset_minutes` | 截止 = 触发时刻 + 该分钟数（1 天=1440，1 周=10080） |
| `chain_after_complete` | true 时用户完成当期后立即生成下一期；false 只按触发时刻生成 |
| `next_trigger_at` | 下次触发时间，生成后自动滚动 |

例：每周五 12:00 触发「刷本周网课」、本周日 12:00 截止
→ `WEEKLY` + `dayOfWeek=5` + `12:00` + `ddlOffsetMinutes=2880`

- `RecurringScheduleCalculator` 负责触发时间推算与校验
- `RecurringTodoScheduler` 每 60s 扫描一次；启动时补扫一次，
  补齐停机期间错过的触发（单规则上限 `MAX_CATCH_UP=5` 期，避免刷屏）
- 删除规则时**保留已生成的待办**，仅把 `recurring_id` 置空
  （MyBatis-Plus 的 `updateById` 忽略 null，这里用 `lambdaUpdate().set(...)` 显式置空）
- 循环规则归入 `SCOPE_TODO` 作用域，其内容参与待办域指纹计算

### 10.1 自定义循环规则（frequency=CUSTOM）

固定的每日/每周/每月覆盖不了「双周的周五」「每月最后一个工作日」这类需求，
因此支持用户写脚本决定触发时机。

**为什么用 JavaScript（Rhino）而不是 Python**
JVM 内嵌 Python（Jython 停滞在 2.7、GraalPy 体积大）沙箱难做；
Rhino 提供 `ClassShutter` + 指令计数 + 栈深限制，可在进程内安全隔离，
且 JS 对前端用户更友好、编辑器高亮现成。

**契约**：用户脚本必须定义 `shouldTrigger(ctx)` 并返回**布尔值**
（返回数字/字符串会被拒绝，避免 `"0"` 之类隐式转换造成误触发）。

`ctx` 只包含纯数据，不暴露任何 Java 对象：

| 字段 | 说明 |
|------|------|
| `year` / `month` / `day` | 年 / 月 / 日 |
| `dayOfWeek` | 1=周一 … 7=周日 |
| `hour` / `minute` | 时 / 分 |
| `weekOfYear` | ISO 周序号（判断单双周用） |
| `dayOfYear` / `daysInMonth` | 年内第几天 / 当月天数 |
| `isLastDayOfMonth` / `isWeekend` | 是否月末 / 是否周末 |
| `daysSinceLastTrigger` | 距上次触发天数（从未触发为 -1） |
| `neverTriggered` | 是否从未触发 |

**沙箱五层防护**（`RecurringScriptEvaluator`，均已逐项攻击验证）

| 防护 | 拦截目标 |
|------|---------|
| `ClassShutter` 全量拒绝 | `java.lang.Runtime` / `Packages` / `java.io.File` → ReferenceError |
| 指令计数（每 200 条观察） | 死循环、大数组分配；用 `Error` 抛出，脚本无法 try/catch 吞掉 |
| `setMaximumInterpreterStackDepth(256)` | 无限递归 |
| 堆增长哨兵 64MB + 工作线程内捕获 OOM/StackOverflow | 内存耗尽拖垮服务 |
| `initSafeStandardObjects(sealed)` + 4000 字符上限 | 桥接逃逸、超大脚本 |

> **踩坑记录**：最初只做了前两层，测试发现无限递归
> （`function f(){return f();}`）会让 Rhino 解释器堆帧耗尽内存，
> 抛出的 `OutOfMemoryError` 从工作线程传播到主线程，**整个 JVM 崩溃**。
> 修复方式是栈深限制 + 在工作线程内捕获 `OutOfMemoryError`。
> 结论：执行不可信代码时，光靠指令计数不够，必须同时限制内存与栈深。

**运行机制**
- CUSTOM 不预算 `next_trigger_at`（`nextTriggerAfter` 返回 null），
  调度器每分钟执行一次脚本判断
- 去重靠「同一分钟内不重复触发」（`last_triggered_at` 截断到分钟比较），
  因为 60s 周期可能在同一分钟被扫到两次
- **保存前强制试运行**，语法错误或不返回布尔值直接拒绝入库
- 运行期出错的规则**自动停用**，避免每分钟反复报错刷日志

---

## 11. 考试记录

考试记录用于期末周等场景，存在 `exam` 表，**不按课时计算时间**，直接输入日期与起止时间。

| 字段 | 说明 |
|------|------|
| `schedule_id` | 所属课表（归属/权限判定） |
| `name` | 考试名；直接用考试名称/类型（如「数据结构期末考试」「CET-6」），不关联课程 |
| `exam_date` / `start_time` / `end_time` | 直接输入的日期与起止时间 |
| `location` | 地点，可空 |

### 课表展示

- 所有考试在网格中**都像独立考试一样渲染**：在 `exam_date` 当天、
  按其起止时间对齐到作息节次区间，单独占一格**红色块**（`.exam-block`，非课程、固定红色）
- 考试块只显示**考试名称、起止时间（两行）、地点**，不关联/不显示课程
- 点击考试块进入考试管理
- `TimetableGrid.examPeriods` 把起止时间近似映射到节次区间用于占位

### 自动关联待办

创建/修改/删除考试时，`ExamServiceImpl` 会同步操作一条关联待办（`todo.exam_id`）：
- **创建** → 自动生成待办，`ddl = 考试开始时间`，标题=考试名
- **修改** → 同步关联待办的标题与截止时间
- **删除** → 一并删除关联待办
- **`ExamTodoScheduler`** 每 60s 扫描一次（启动补扫），考试结束
  （`examDate + endTime` 已过）后自动把关联且未完成的待办标记完成（幂等）
- 前端待办列表里，考试关联待办显示红色「考试」标签（类似循环待办的「循环」标签）

> 注意：`todo.exam_id` 是新增列，生产库需手动 `ALTER TABLE todo ADD COLUMN exam_id BIGINT NULL`
> （生产 `application-prod.yml` 用 `sql.init.mode=always`，`CREATE IF NOT EXISTS` 不会给
> 已存在的表补列；`exam` 表可由 schema.sql 自动创建）。

### AI 关联

考试归入 `SCOPE_COURSE` 作用域，`CREATE_EXAM / UPDATE_EXAM / DELETE_EXAM` 可被 AI 生成并执行；
`UPDATE_EXAM / DELETE_EXAM` 的 `data._before` 含修改前快照，前端差异卡只展示变化字段。

---

## 12. 前端体验

### 响应式布局（窄屏适配）

- 断点 `≤900px`：**侧边栏**（AppLayout）与 **AI 对话列表**（AiChatPage）变为左侧抽屉
  （默认移出屏幕，点汉堡/「对话」按钮滑入 + 半透明遮罩）；内容区 padding 收窄
- 断点 `≤640px`（手机）：课表网格列宽自动收缩（`minmax(0,1fr)`）**不横向滚动**、
  弹窗贴顶限宽、页头纵向堆叠、聊天输入纵向堆叠
- 课表网格 `height:100%` + `grid-auto-rows: minmax(48px,1fr)` **占满剩余高度**不留底部空隙；
  节次侧栏时间两行、字号缩小；考试块时间两行
- 全部课程页为**卡片式**（`.course-card-grid` 响应式网格，非表格，天然无横向溢出）

### 字体大小设置（FontContext）

- `store/FontContext.tsx`：两档字号存 localStorage（`timetable.fontScaleGlobal` /
  `timetable.fontScaleTimetable`），通过 CSS 变量 `--app-font-scale` 与 `--tt-scale` 生效
- 全局字体：写入 antd `ConfigProvider` 的 `fontSize` token（theme.tsx）+ body `font-size: calc(13px * var(--app-font-scale))`
- 课表字体：课表网格各字号用 `em`（相对 `calc(12px * var(--tt-scale))` 基值），改基值即可整体缩放
- 设置页提供两个滑块（0.8–1.4×）+ 重置按钮

### 作息时间表访问控制

- 仅 label 为 **`Tangent0712`** 的账号可编辑作息时间表；其余用户：
  顶部显示红色只读提示「这是南京邮电大学标准作息时间表，仅供阅读，不可编辑！」、
  隐藏保存按钮、控件不变灰但不可点击（`open={false}` / `inputReadOnly`）
- 判断基于前端 `label === 'Tangent0712'`（简单校验，产品定位为好友自用）

---

## 13. 注意事项

1. **多用户设计**: 每个 API Key 独立数据空间，Key 由管理员在数据库 `api_key` 表预设
2. **Course.weeks**: MySQL JSON 类型，MyBatis-Plus 用 `JacksonTypeHandler` 映射为 `List<Integer>`
3. **CORS**: `WebConfig` 中对 `/api/**` 放开了所有来源
4. **事务**: `BatchCourseRequest` 批量添加课程标注了 `@Transactional`
5. **Schedule GET /{id}**: 返回 `{schedule: {...}, courses: [...]}` 组合结构
6. **考试不关联课程**: `exam` 表已删除 `course_id` 列（本地与生产库均已 ALTER），
   考试只含名称/日期/起止时间/地点，网格中统一渲染为独立红色块
7. **文档**: 用户使用手册见 `docs/USER_MANUAL.md`（给非技术朋友），前端内置「用户手册」页 `/manual`
   