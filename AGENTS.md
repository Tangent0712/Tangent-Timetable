# AGENTS.md — TimeTable 项目开发指南

> 面向接手的 AI Agent，快速了解项目全貌、约定和当前进度。

---

## 1. 项目概述

**大学生课程表+待办事项** 一站式日程管理工具，覆盖 Web / Android / macOS 三端。

- **Git 仓库**: `main` 分支，2 个 commit
- **详细需求**: `docs/PRD.md` (v0.5)
- **Android 灵动岛技术指南**: `docs/SUPER_ISLAND_INTEGRATION_GUIDE.md`

---

## 2. 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL 8.x |
| 构建 | Maven (wrapper 未配置，用系统 mvn) |
| 认证 | `X-API-Key` Header 拦截器 |
| Web 前端 | React + Ant Design *(尚未实现)* |
| Android | Kotlin + Jetpack Compose *(尚未实现)* |
| macOS | SwiftUI + WidgetKit *(尚未实现)* |
| AI | DeepSeek API *(配置预留，功能尚未实现)* |

---

## 3. 项目结构

```
ToDoList- TimeTable/
├── .gitignore
├── docs/
│   ├── PRD.md                              # 产品需求文档
│   └── SUPER_ISLAND_INTEGRATION_GUIDE.md   # 小米超级岛技术指南
├── AGENTS.md                               # 本文件
└── backend/                                # Spring Boot 后端 (已实现)
    ├── pom.xml
    └── src/main/
        ├── java/com/timetable/
        │   ├── TimetableApplication.java      # 入口
        │   ├── config/
        │   │   ├── WebConfig.java             # CORS 配置
        │   │   └── WebMvcConfig.java          # 拦截器注册
        │   ├── controller/
        │   │   ├── AuthController.java        # /api/auth
        │   │   ├── ScheduleController.java    # /api/schedules
        │   │   ├── CourseController.java      # /api/courses, /api/schedules/{id}/courses
        │   │   ├── TodoController.java        # /api/todos
        │   │   └── PeriodConfigController.java # /api/period-config
        │   ├── dto/
        │   │   ├── ApiResponse.java           # 统一响应 {code, message, data}
        │   │   ├── AuthVerifyResponse.java
        │   │   ├── ScheduleRequest.java / CourseRequest.java
        │   │   ├── TodoRequest.java / PeriodConfigRequest.java
        │   │   └── BatchCourseRequest.java
        │   ├── entity/
        │   │   ├── ApiKey.java                # api_key 表
        │   │   ├── Schedule.java              # schedule 表
        │   │   ├── Course.java                # course 表
        │   │   ├── Todo.java                  # todo 表
        │   │   └── PeriodConfig.java          # period_config 表
        │   ├── mapper/                        # MyBatis-Plus Mapper (5个)
        │   ├── service/
        │   │   ├── ApiKeyService.java 等       # 5 个 service 接口
        │   │   └── impl/                      # 5 个 service 实现
        │   ├── interceptor/
        │   │   ├── ApiKeyInterceptor.java     # X-API-Key 认证
        │   │   └── RequestContext.java        # ThreadLocal 上下文
        │   └── exception/
        │       ├── BusinessException.java
        │       └── GlobalExceptionHandler.java
        └── resources/
            ├── application.yml
            └── db/
                ├── schema.sql                 # 建表 DDL
                └── data.sql                   # 默认数据 (1个demo key + 12节作息)
```

---

## 4. 开发命令

```bash
# 编译 (需先装好 Java 17+ 和 Maven)
cd backend
mvn compile

# 运行 (需先启动 MySQL)
mvn spring-boot:run

# 打包
mvn package -DskipTests

# 跳过测试编译
mvn package -DskipTests
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
| P1 AI 集成 | DeepSeek 解析HTML、自然语言CRUD、多轮对话 | *未开始* |
| P2 Web 前端 | React + Ant Design 课表视图、AI 输入框 | *未开始* |
| P3 Android | 课表查看、待办查看、同步 | *未开始* |
| P4 Android 小组件 | Glance 4x6 Widget、DDL倒计时 | *未开始* |
| P5 Android 灵动岛 | FocusNotification + Shizuku + LiveUpdate | *未开始* |
| P6 Mac 小组件 | SwiftUI Notification Center Widget | *未开始* |

---

## 9. AI 接口预留

`application.yml` 中已配置 DeepSeek API 参数：

```yaml
deepseek:
  api:
    key: ${DEEPSEEK_API_KEY:your-deepseek-api-key}
    base-url: https://api.deepseek.com
    model: deepseek-chat
```

PRD 定义的 AI 接口路径（尚未实现）：
- `POST /api/ai/conversations`
- `POST /api/ai/conversations/{id}/messages`
- `GET /api/ai/conversations/{id}/messages`
- `POST /api/ai/conversations/{id}/messages/{msgId}/execute`

---

## 10. 注意事项

1. **多用户设计**: 每个 API Key 独立数据空间，Key 由管理员在数据库 `api_key` 表预设
2. **Course.weeks**: MySQL JSON 类型，MyBatis-Plus 用 `JacksonTypeHandler` 映射为 `List<Integer>`
3. **CORS**: `WebConfig` 中对 `/api/**` 放开了所有来源
4. **事务**: `BatchCourseRequest` 批量添加课程标注了 `@Transactional`
5. **Schedule GET /{id}**: 返回 `{schedule: {...}, courses: [...]}` 组合结构
