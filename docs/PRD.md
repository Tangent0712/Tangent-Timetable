# 大学生课程表+待办事项 产品需求文档 (PRD)

> 版本: v0.5 | 状态: 待审查

---

## 1. 产品概述

### 1.1 产品定位
面向个人大学生的一站式日程管理工具，覆盖 **Web / Android / macOS** 三端，以 AI 降低课表录入和维护成本，以实时同步和多端小组件提升使用效率。

### 1.2 核心价值
- **AI 零门槛录入**：复制教务系统 HTML → AI 解析自动建课表；自然语言一句话增删改课程/待办
- **三端无缝同步**：服务器统一存储，所有终端数据一致
- **端侧深度集成**：Android 桌面小组件 + 灵动岛进度条 + 上课提醒；Mac 通知中心小组件

---

## 2. 用户画像

| 属性 | 描述 |
|------|------|
| 用户数量 | **小规模多人**（自用 + 可分享给朋友），每人一个独立 API Key 作为"账号" |
| 设备 | MacBook（macOS）、小米手机（Android）、任意浏览器 |
| 使用场景 | 学期初导入课表 → 日常查看课表/添加待办 → 上课前手机提醒 + 实时进度条 |

---

## 3. 系统架构总览

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Web 前端     │     │ Android 客户端 │     │  Mac 客户端   │
│  (浏览器)     │     │  (原生 Kotlin) │     │  (SwiftUI)   │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │
       └────────────────────┼────────────────────┘
                            │  REST API
                   ┌────────┴────────┐
                   │   后端服务 (Java)  │
                   │   Spring Boot   │
                   └────────┬────────┘
                            │
                   ┌────────┴────────┐
                   │     MySQL       │
                   └─────────────────┘
                            │
                   ┌────────┴────────┐
                   │  DeepSeek API   │
                   │  (AI 解析/命令)  │
                   └─────────────────┘
```

---

## 4. 功能需求

> **API Key 作为账号体系**：每人分配一个独立的 API Key，所有请求在 HTTP Header 中携带 `X-API-Key`，后端根据 Key 识别用户并隔离数据。Key 由管理员在服务端配置文件/数据库中预设，或通过 Web 管理页创建。

### 4.1 后端服务 (Java)

#### 4.1.1 数据模型

##### API Key (账号)
| 字段 | 类型 | 说明 |
|------|------|------|
| api_key | VARCHAR(64) | 主键，密钥即账号 |
| label | VARCHAR(50) | 显示名，如 "我的" / "小明" |
| created_at | DateTime | 创建时间 |

> 每个 API Key 对应一个独立用户的数据空间。Key 由管理员预设。所有业务数据（Schedule / Todo）通过 `api_key` 字段隔离。

##### 课表 (Schedule)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| api_key | VARCHAR(64) | 所属用户 |
| name | String | 课表名称，如 "2025-2026秋季学期" |
| period_start_date | Date | 学期开始日期 |
| period_end_date | Date | 学期结束日期 |
| created_at | DateTime | 创建时间 |
| updated_at | DateTime | 更新时间 |

##### 作息时间表 (PeriodConfig) — 全局固定配置
| 字段 | 类型 | 说明 |
|------|------|------|
| period_number | Int(1-12) | 节次 |
| start_time | Time | 开始时间 如 08:00 |
| end_time | Time | 结束时间 如 08:45 |
| category | Enum | MORNING(1-5) / AFTERNOON(6-9) / EVENING(10-12) |

> 默认作息时间表在服务端配置文件中预设，用户可在 Web 端修改。

##### 课程 (Course)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| schedule_id | Long | 所属课表 |
| name | String | 课程名称 |
| location | String | 上课地点 |
| teacher | String | 教师名称 |
| day_of_week | Int(1-7) | 星期几 (1=周一) |
| start_period | Int(1-12) | 开始节次 |
| end_period | Int(1-12) | 结束节次 (必须 >= start_period) |
| weeks | JSON / 位掩码 | 上课周次，如 [1,2,3,...,16] |
| created_at | DateTime | 创建时间 |
| updated_at | DateTime | 更新时间 |

**说明**：每一门课的每一次上课时间是一条独立记录。例如"高等数学 周一1-3节 1-16周" 和 "高等数学 周三1-3节 1-16周" 是两条 Course 记录，共享相同的 course 名称。

##### 待办事项 (Todo)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| api_key | VARCHAR(64) | 所属用户 |
| title | String | 事项标题 |
| ddl | DateTime | 截止时间 |
| completed | Boolean | 是否完成 |
| recurring_id | Long | 可空；由循环规则自动生成时指向该规则 |
| exam_id | Long | 可空；由考试自动关联生成（ddl=考试开始时间，考试结束后自动完成） |
| created_at | DateTime | 创建时间 |
| updated_at | DateTime | 更新时间 |

##### 考试记录 (Exam) — 期末周/四六级等
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| schedule_id | Long | 所属课表（归属/权限） |
| name | String | 考试名称/类型，如「数据结构期末考试」「CET-6」，不关联课程 |
| location | String | 地点，可空 |
| exam_date | Date | 考试日期 |
| start_time / end_time | Time | 起止时间（**不按课时，直接输入**） |
| created_at / updated_at | DateTime | 时间戳 |

> 考试**不关联课程**。创建/修改/删除考试时自动同步一条关联待办（`todo.exam_id`）；
> 考试结束后由 `ExamTodoScheduler` 自动完成该待办。课表网格中考试按日期/时间渲染为独立红色块，
> 只显示考试名称、起止时间（两行）、地点。

##### 循环待办规则 (RecurringTodo)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| api_key | VARCHAR(64) | 所属用户 |
| title | String | 规则标题 |
| frequency | Enum | DAILY / WEEKLY / MONTHLY / CUSTOM |
| day_of_week / day_of_month | Int | WEEKLY / MONTHLY 用 |
| trigger_time | Time | 每期触发时刻（CUSTOM 不需要） |
| script | Text | CUSTOM 模式的 `shouldTrigger(ctx)` 脚本 |
| ddl_offset_minutes | Int | 截止 = 触发时刻 + 该分钟数 |
| enabled / chain_after_complete | Boolean | 是否启用 / 完成后是否立即接下一期 |
| next_trigger_at | DateTime | 下次触发时间 |

> 到点由 `RecurringTodoScheduler` 自动生成真实待办（`todo.recurring_id` 指回规则）；
> CUSTOM 用 Rhino 沙箱执行脚本判断触发时机（双周周五、每月最后工作日等）。

#### 4.1.2 核心接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/auth/verify | 验证 API Key 有效性，返回 label |
| GET | /api/schedules | 获取所有课表 |
| POST | /api/schedules | 新建课表 |
| GET | /api/schedules/{id} | 获取课表详情（含课程列表） |
| PUT | /api/schedules/{id} | 修改课表信息 |
| DELETE | /api/schedules/{id} | 删除课表 |
| GET | /api/schedules/{id}/courses | 获取指定课表的所有课程 |
| POST | /api/schedules/{id}/courses | 添加课程 |
| POST | /api/schedules/{id}/courses/batch | 批量添加课程 |
| PUT | /api/courses/{id} | 修改单条课程 |
| DELETE | /api/courses/{id} | 删除单条课程 |
| GET | /api/todos | 获取待办列表 |
| POST | /api/todos | 创建待办 |
| PUT | /api/todos/{id} | 修改待办 |
| DELETE | /api/todos/{id} | 删除待办 |
| GET | /api/schedules/{id}/exams | 获取课表下所有考试 |
| POST | /api/schedules/{id}/exams | 新增考试 |
| PUT | /api/exams/{id} | 修改考试 |
| DELETE | /api/exams/{id} | 删除考试 |
| GET/POST/PUT/DELETE | /api/recurring-todos[/{id}] | 循环待办规则 CRUD |
| GET | /api/period-config | 获取作息时间表 |
| PUT | /api/period-config | 修改作息时间表（仅 Tangent0712 前端可改） |
| POST | /api/ai/conversations | 创建 AI 对话 |
| POST | /api/ai/conversations/{id}/messages | 发送消息，返回 AI 回复（含 action 提案） |
| GET | /api/ai/conversations/{id}/messages | 获取对话历史 |
| POST | /api/ai/conversations/{id}/actions/{actionId}/execute | 确认执行单个 AI 提案 |

---

### 4.2 Web 前端

#### 4.2.1 功能清单

| 模块 | 功能 | 说明 |
|------|------|------|
| 课表管理 | 新建/切换/删除课表 | 支持多个学期课表并存 |
| 课表管理 | 导入教务系统HTML | 粘贴HTML → 调用AI解析 → 预览 → 确认导入（**仅Web端支持**） |
| 课表管理 | 可视化课表视图 | 按周视图显示，每格显示课程名+地点+教师；窄屏自动收缩、占满屏幕 |
| 课表管理 | 手动增删改课程 | 表单编辑课程详情 |
| 课表管理 | 全部课程卡片视图 | 非表格、响应式卡片网格，无横向滚动 |
| 考试管理 | 考试增删改 | 直接输入日期/起止时间，不关联课程；课表红色块展示；自动生成关联待办 |
| 待办管理 | 待办列表 | 显示标题、剩余时间、完成状态、循环/考试标签 |
| 待办管理 | 循环任务 | 每日/每周/每月/自定义脚本规则，到点自动生成待办 |
| AI 操作 | 自然语言操作课表/待办/考试 | 一句话增删改查，多步骤操作逐条卡片确认 |
| 设置 | 作息时间表 | **仅 Tangent0712 可编辑**，其余只读（红色提示） |
| 设置 | 字体大小 | 全局字体 + 课表字体两档，保存到本机 localStorage |
| 帮助 | 用户手册页 | 内置 `/manual` 白话教程（给非技术朋友） |

#### 4.2.2 关键交互

- **课表导入流程**：点击"导入课表" → 弹出文本框粘贴HTML → 点击"AI解析" → 展示解析结果列表（课程名/地点/教师/时间/周次）→ 用户确认/修正 → 确认导入
- **自然语言输入框**：页面顶部固定一个 AI 输入框，支持输入自然语言指令

---

### 4.3 Android 客户端（小米）

#### 4.3.1 技术约束
- 原生 Android 开发（Kotlin）
- 无小米开发者权限，通过 **Shizuku** 调用隐藏 API（临时断网 XMSF 包）使通知被 HyperOS 识别为可信 FocusNotification
- 灵动岛分两种模式：
  - **小米超级岛（FocusNotification）**：通过 `focus-api` 库 + Shizuku XMSF 绕过，显示进度条/课程信息。需 MIUI/HyperOS + Shizuku 已授权。
  - **LiveUpdate（降级方案）**：标准 Android 前台服务通知，Android 16+ 系统自动在灵动岛区域显示。无超级岛时自动降级。

#### 4.3.2 功能清单

| 模块 | 功能 | 说明 |
|------|------|------|
| 桌面小组件 | 4x6 桌面 Widget | 占满一整页，显示今日课表 + 待办事项 |
| 桌面小组件 | 待办DDL实时倒计时 | 显示精度到秒，两元素格式：`1天4小时` / `7分钟45秒` |
| 桌面小组件 | 今日课表展示 | 按时间顺序显示当天课程 |
| 桌面小组件 | 自动刷新 | 定时同步服务端数据 |
| 灵动岛 | 上课中进度条 | 当前课程实时进度条（已上课时/总课时），上课开始～下课结束期间始终显示 |
| 灵动岛 | 课前通知 | 上课前 N 分钟（可配置，默认15分钟）弹岛提醒 |
| 灵动岛 | 下课前通知 | 下课前 N 分钟（可配置，默认5分钟）弹岛提醒 |
| 灵动岛 | 下节课预告 | 课间显示下一节课信息 |
| 通知 | 待办DDL提醒 | 截止时间前通知 |
| 基础页面 | 课表查看 | 周视图课表 |
| 基础页面 | 待办列表 | 查看/完成待办 |
| AI 操作 | 自然语言操作课表 | 同 Web 端，支持一句话增删改查课程 |
| AI 操作 | 自然语言添加待办 | 同 Web 端，支持一句话创建待办 |
| 同步 | 实时同步 | 与后端保持数据同步 |

#### 4.3.3 灵动岛实现（基于 SUPER_ISLAND_INTEGRATION_GUIDE.md）

##### 核心依赖

| 依赖 | 用途 |
|------|------|
| `com.xzakota.hyper.notification:focus-api:1.4` | 小米超级岛 FocusNotification DSL 构建 |
| `dev.rikka.shizuku:api:13.1.5` / `provider:13.1.5` | Shizuku 权限调用 |
| `org.lsposed.hiddenapibypass:hiddenapibypass:6.1` | 隐藏 API 绕过 |

##### 两种模式

| 模式 | 条件 | 能力 |
|------|------|------|
| **小米超级岛** | HyperOS + Shizuku 已授权 + 设备支持 FocusNotification | 自定义布局、进度条、AOD 标题 |
| **LiveUpdate（降级）** | 不满足上述条件时自动降级 | 标准通知在灵动岛区域显示标题+副标题 |

##### 上课进度显示流程

```
上课前N分钟 → 发通知预告
上课时      → 每隔30s更新一次通知，setContentTitle = "课程名", progressInfo.progress = 已过分钟/总分钟*100
下课前N分钟 → 更新通知内容为下课提醒
下课时      → 取消通知
```

##### 通知字段映射

| 显示内容 | FocusNotification 字段 |
|----------|----------------------|
| 胶囊主文本 | `ticker` + `setContentTitle()` |
| 展开态课程名 | `chatInfo.title` / `bigIslandArea.textInfo.title` |
| 展开态详情 | `chatInfo.content`（地点+教师） |
| 进度百分比 | `progressInfo.progress` (0-100) |
| AOD 熄屏标题 | `aodTitle`（≤20字） |

##### XMSF 绕过发送流程（STANDARD 模式）

```
1. 构建 FocusNotification Bundle（focus-api DSL）
2. 构建标准 Notification，addExtras(focusExtras)
3. 通过 Shizuku 临时断网 XMSF（com.xiaomi.xmsf）
4. notificationManager.notify() 发送通知
5. 延迟 100ms 后恢复 XMSF 网络
```

##### 调度策略

| 事件 | 触发时机 | 动作 |
|------|----------|------|
| 课前预告 | 上课前 N 分钟（默认15） | 发送超级岛通知，显示"即将上课：课程名"，progress=0 |
| 上课开始 | 上课时间到达 | 进度条开始，progress 从 0 递增 |
| 上课中 | 每 30s 更新 | 更新 progress = 已过分钟/总分钟 × 100 |
| 下课前提醒 | 下课前 N 分钟（默认5） | 更新通知内容，提示即将下课 |
| 下课 | 下课时间到达 | 取消通知 |

##### 降级行为

当设备不满足超级岛条件时，自动降级为 LiveUpdate 模式：通过标准前台服务通知，`setContentTitle` = 课程名，`setContentText` = 进度/详情，Android 16+ 系统灵动岛自动展示。

> 详细实现代码参见 `docs/SUPER_ISLAND_INTEGRATION_GUIDE.md`。

#### 4.3.4 桌面小组件设计

```
┌──────────────────────────────────────┐
│  📅 今日课表           2025-09-15 周一 │
│                                      │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  │
│  ▓▓▓ 8:00-9:40 高等数学  教3-101 ▓▓▓ │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  │
│                                      │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│  ░░░ 10:00-11:40 大学英语 教1-205 ░░░│
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  │
│                                      │
│  ─────────────────────────────────   │
│  📋 待办事项                         │
│                                      │
│  ⏰ 高数作业    剩余 1天4小时        │
│  ⏰ 实验报告    剩余 4小时56分钟     │
│  ⏰ 英语作文    剩余 7分钟45秒       │
│                                      │
│  上次更新: 刚刚                      │
└──────────────────────────────────────┘
```

---

### 4.4 Mac 客户端

#### 4.4.1 方案选择
- macOS 通知中心 Widget（WidgetKit / SwiftUI），固定在通知中心侧栏，与系统原生体验一致

#### 4.4.2 功能清单

| 功能 | 说明 |
|------|------|
| 今日课表 | 按时间顺序展示当天课程 |
| 待办列表 + DDL倒计时 | 同步显示待办，精度同 Android 小组件 |
| 自动刷新 | 定时与服务端同步 |
| 点击交互 | 点击待办标记完成（或跳转Web端） |

#### 4.4.3 设计参考

```
┌──────────────────────┐
│ 📅 今日课表           │
│ 8:00-9:30 高等数学        │
│ 1-2 教3-101 王老师   │
│ 10:00-12:00 大学英语        │
│ 3-5 教1-205 李老师   │
│ ────────────────────  │
│ 📋 待办               │
│ ⏰ 高数作业           │
│   1天4小时 ddl2.24 4:40  │
│ ⏰ 实验报告           │
│   4小时56分钟 ddl2.24 4:40  │
│ ⏰ 英语作文           │
│   7分钟45秒 ddl2.24 4:40  │
└──────────────────────┘
```

---

### 4.5 AI 赋能（DeepSeek API）

#### 4.5.1 功能场景

| 场景 | 输入 | AI理解 | 说明 |
|------|------|------|------|
| 解析教务系统HTML | 粘贴HTML源码 | 结构化课程列表 | 一次性解析，返回预览供确认导入 |
| 新增课程 | "下周一3-4节加一节形势政策 地点待定" | 下周一（单次）加课 | 仅添加下一周当天那一节课；若说"每周一"则是全学期 |
| 修改课程 | "把周三的高数从第3-4节改到第5-6节" | 修改指定课程的节次 | 定位已有课程并修改 |
| 删除课程 | "去掉周五上午所有的课" | 批量删除符合条件的课程 |  |
| 调休场景 | "这周六补下周一的课" | 复制周一课程到周六（仅该周六） | 临时调课 |
| 添加待办 | "下周五下午5点前交高数作业" | 提取标题+DDL |  |

**关键语义约定**：
- "下周一" = 下一周的那个周一（单日），不是每周一
- "每周一" / "每周的周一" = 所有周一
- "周三"（无修饰） = 本学期所有周三
- 位置信息、教师信息未提供时设为空，AI 回复中标注"待补充"

#### 4.5.2 多轮对话机制

AI 功能不是单次命令，而是**多轮对话交互**，类似主流 AI 聊天界面：

```
用户: "下周一3-4节加一节形势政策"

AI: "已为你创建课程卡片，请确认后点击确认添加。"

用户: "地点暂定教3-205"

AI: "已更新地点为教3-205。"

[用户点击卡片上的"确认添加"] → 执行，AI回复"已添加。"
```

> 确认执行仅通过卡片按钮操作，不接受自然语言确认（避免 AI 误判）。

##### AI 回复类型

| 类型 | 说明 | 前端展示 |
|------|------|---------|
| **操作提案** | AI 解析出待执行的 CRUD 操作 | 卡片形式展示操作详情 + "确认执行"按钮 |
| **查询结果** | AI 查询并展示课表/待办信息 | 文本/表格形式展示 |
| **追问** | 信息不完整，AI 追问缺失字段 | 文本 + 可内联补充的输入框 |
| **纯文本** | 一般对话回复 | 普通气泡 |

#### 4.5.3 后端接口

```
POST /api/ai/conversations
创建新对话，返回 conversationId

POST /api/ai/conversations/{id}/messages
{
  "scheduleId": 1,
  "content": "下周一3-4节加一节形势政策"
}

Response:
{
  "messageId": "msg_001",
  "role": "assistant",
  "text": "好的，我解析到以下课程信息：课程：形势政策，时间：下周一...",
  "action": {                          // 可选，有操作提案时才返回
    "type": "CREATE_COURSE",
    "status": "PENDING",              // PENDING=待确认, EXECUTED=已执行, REJECTED=已拒绝
    "data": {
      "name": "形势政策",
      "dayOfWeek": 1,
      "startPeriod": 3,
      "endPeriod": 4,
      "location": null,
      "teacher": null,
      "weeks": [5]                    // 当前是第4周，"下周一"=第5周周一
    }
  }
}

POST /api/ai/conversations/{id}/actions/{actionId}/execute
确认执行 AI 提议的单个操作，后端执行 CRUD 并写库

GET /api/ai/conversations/{id}/messages
获取对话历史
```

> **AI 操作提案**：AI 返回 `{text, actions[]}`，一次可含多个操作（如「删除所有考试」→ 多张「删除考试」卡）。
> 每条数据库基本操作独立成一张卡片、各自有「确认执行/取消」按钮，**逐条确认，不做一键全部执行**。
> 支持的 action.type：课程（CREATE/UPDATE/DELETE_COURSE）、待办（CREATE/UPDATE/DELETE/TOGGLE_TODO）、
> 循环（CREATE/UPDATE/DELETE/TOGGLE_RECURRING）、考试（CREATE/UPDATE/DELETE_EXAM）。
> 若 AI 把多条记录塞进同一 action，后端 `splitToSingleActions` 会按数组字段逐条拆成独立 action。
> 每个 AI 对话绑定一个课表作为**工作空间**，非该课表下禁止发消息/执行（防止串数据）。

#### 4.5.4 AI Prompt 设计要点
- System Prompt 需包含以下上下文信息：
  - **当前日期与学期对照表**：每次请求附带当前真实日期（年月日、星期几），以及"x月x日 对应 第x周 周x"的完整映射表
  - **学期信息**：当前学期的起止日期、周数范围
  - **已有课程数据**：每次发送消息时，后端根据 `scheduleId` 从数据库实时查询该课表的完整课程列表，注入到 Prompt 上下文中，供 AI 在修改/删除时定位目标
  - **已有待办数据**：同样实时查询全部待办列表注入
- Prompt 中明确告知 AI "今天的日期是 xxxx年x月x日 周x，明天是 xxxx年x月x日 周x"，使 AI 能准确理解"明天""下周一""这周五"等相对时间表述
- 返回严格的 JSON 格式，包含 `text`（自然语言回复）+ `action`（可选，操作提案）
- 不确定的字段设为 null，在 text 中追问用户
- 保持对话上下文，支持用户在多轮对话中逐步补全信息

#### 4.5.5 Web 前端 AI 对话页面

- 独立 AI 对话页面，类似 ChatGPT / DeepSeek 聊天界面
- **左侧/顶部**：当前激活的课表名称 + 学期信息
- **对话区**：消息气泡列表，AI 的"操作提案"渲染为卡片 + 操作按钮
- **操作卡片**：展示 `name / dayOfWeek / periods / location / teacher / weeks` 等字段，缺失字段标注为灰色"未指定"
- **操作按钮**：
  - 「确认执行」→ 调用 execute 接口，执行后卡片状态变为"已执行"（绿色）
  - 「修改」→ 用户补充信息，继续对话
  - 「取消」→ 拒绝该提案
- 支持多轮对话，完整对话历史可查看

---

### 4.6 数据同步

#### 4.6.1 同步策略

| 策略 | 说明 |
|------|------|
| 服务端为数据源 | 所有数据以服务端 MySQL 为准 |
| 客户端定时拉取 | Android/Mac/Web 每隔 30 秒轮询增量更新 |
| 实时性增强（可选） | 后期可升级为 WebSocket 推送 |

#### 4.6.2 同步范围
- 课程数据：全量同步（单个用户数据量小）
- 待办数据：全量同步
- 作息时间表：按需同步（修改频次极低）

---

### 4.7 登录/鉴权流程（各端）

#### 4.7.1 通用流程

```
客户端启动 → 检查本地是否已存储 API Key
  ├── 有 Key → 调用 GET /api/schedules 验证 → 成功则进入主界面
  └── 无 Key / 验证失败 → 显示输入页面 → 用户输入 Key → 验证 → 存储本地
```

#### 4.7.2 Web 端

- 首次访问显示登录页，输入 API Key
- 验证成功后 Key 存入 `localStorage`
- 所有 API 请求从 `localStorage` 读取 Key 附加到 Header
- 页面提供「登出」按钮，点击后清除 `localStorage` 中的 Key 并跳转回登录页

#### 4.7.3 Android 端

- 首次启动显示 API Key 输入页面
- 验证成功后 Key 存入 `SharedPreferences`
- 后续启动自动使用已存 Key，跳过输入页
- 设置页提供「登出」按钮，清除 Key 后回到输入页

#### 4.7.4 Mac 端

- 首次启动显示 API Key 输入页面
- 验证成功后 Key 存入 `UserDefaults`
- 后续启动自动使用已存 Key
- 设置页提供「登出」按钮

---

## 5. 非功能需求

| 类别 | 要求 |
|------|------|
| 性能 | 单用户场景，所有接口响应 < 500ms |
| 可用性 | 服务端 7x24 运行（个人服务器） |
| 安全 | 每个 API Key 对应独立用户数据空间；所有请求必须在 Header 中携带 `X-API-Key`，后端拦截器校验并注入用户上下文；Key 在服务端预设管理；需防CSRF/XSS |
| 兼容性 | Web: Chrome/Edge/Safari 最新两版；Android: MIUI 13+ / Android 12+；Mac: macOS 12+ |
| 扩展性 | 数据库设计预留多用户可能性（semester → user_id 关联即可扩展） |

---

## 6. 数据库设计

```sql
-- API Keys（账号）
CREATE TABLE api_key (
    api_key VARCHAR(64) PRIMARY KEY,
    label VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 课表
CREATE TABLE schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    period_start_date DATE NOT NULL,
    period_end_date DATE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key) REFERENCES api_key(api_key) ON DELETE CASCADE,
    INDEX idx_api_key (api_key)
);

-- 作息时间表（全局配置，常驻数据）
CREATE TABLE period_config (
    period_number TINYINT PRIMARY KEY,  -- 1-12
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    category ENUM('MORNING','AFTERNOON','EVENING') NOT NULL
);

-- 课程
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    location VARCHAR(200),
    teacher VARCHAR(100),
    day_of_week TINYINT NOT NULL,       -- 1=周一, 7=周日
    start_period TINYINT NOT NULL,       -- 1-12
    end_period TINYINT NOT NULL,         -- 1-12, >= start_period
    weeks JSON NOT NULL,                 -- [1,2,3,...,16]
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (schedule_id) REFERENCES schedule(id) ON DELETE CASCADE,
    INDEX idx_schedule_day (schedule_id, day_of_week)
);

-- 待办
CREATE TABLE todo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    api_key VARCHAR(64) NOT NULL,
    title VARCHAR(500) NOT NULL,
    ddl DATETIME NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (api_key) REFERENCES api_key(api_key) ON DELETE CASCADE,
    INDEX idx_api_key (api_key),
    INDEX idx_ddl (ddl),
    INDEX idx_completed (completed)
);
```

---

## 7. 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.x | Java 17+ |
| 数据库 | MySQL 8.x | 个人服务器部署 |
| ORM | MyBatis-Plus | |
| Web 前端 | React + Ant Design | |
| Android | Kotlin + Jetpack Compose | 原生开发，小组件用 Glance |
| Mac | SwiftUI + WidgetKit | 通知中心小组件 |
| AI | DeepSeek API (deepseek-chat) | 结构化输出 JSON Mode |
| 移动端工具 | Shizuku + focus-api:1.4 + hiddenapibypass | 小米超级岛 XMSF 绕过 |
| 部署 | Docker / 直接运行 jar | 个人服务器 |

---

## 8. 开发计划（建议）

| 阶段 | 内容 | 预估 |
|------|------|------|
| P0 后端核心 | 数据库建表、CRUD API、作息时间表API | 后端先行 |
| P1 AI 集成 | DeepSeek 解析HTML、自然语言CRUD | 核心差异化 |
| P2 Web 前端 | 课表视图、待办列表、AI输入框、导入流程 | 主操作端 |
| P3 Android | 课表查看、待办查看、同步 | 查看端基础 |
| P4 Android 小组件 | 4x6 Widget、DDL倒计时、自动刷新 | |
| P5 Android 灵动岛 | FocusNotification + Shizuku XMSF绕过 + LiveUpdate降级 |
| P6 Mac 小组件 | Notification Center Widget | |

---

## 9. 待定项 & 需要用户确认

| # | 问题 | 状态 |
|---|------|------|
| — | *所有已知问题已确认完毕* | ✅ |

---

## 10. 附录：默认作息时间表参考

| 节次 | 时间 | 类别 |
|------|------|------|
| 1 | 08:00 - 08:45 | MORNING |
| 2 | 08:50 - 09:35 | MORNING |
| 3 | 09:50 - 10:35 | MORNING |
| 4 | 10:40 - 11:25 | MORNING |
| 5 | 11:30 - 12:15 | MORNING |
| 6 | 13:45 - 14:30 | AFTERNOON |
| 7 | 14:35 - 15:20 | AFTERNOON |
| 8 | 15:35 - 16:20 | AFTERNOON |
| 9 | 16:25 - 17:10 | AFTERNOON |
| 10 | 18:30 - 19:15 | EVENING |
| 11 | 19:25 - 20:10 | EVENING |
| 12 | 20:20 - 21:05 | EVENING |

> 以上为默认作息时间表，用户可在 Web 端修改。系统以当前存储的作息时间表为准。

---

*文档版本: v0.6 | 已实现 Web 端全量功能*

### 变更记录
| 版本 | 变更内容 |
|------|----------|
| v0.1 | 初稿 |
| v0.2 | 确定技术选型 (React + MyBatis-Plus)；Mac 确定通知中心 Widget；API Key 安全方案；Android 增加 AI 自然语言操作；AI Prompt 补充日历上下文；更新作息时间表为实际数值；清理已确认项 |
| v0.3 | 基于 SUPER_ISLAND_INTEGRATION_GUIDE.md 更新灵动岛实现方案：FocusNotification+Shizuku XMSF绕过、LiveUpdate降级、调度策略细化；移除 AI Prompt 中作息时间表；全部待定项已确认 |
| v0.4 | AI 重构为多轮对话模式：基于 conversation/messages/execute 接口设计；AI 回复支持操作提案+追问+纯文本三种类型；前端新增独立 AI 对话页面；明确"下周一/每周一/周三"语义约定 |
| v0.5 | 支持多人使用：API Key 作为账号体系，每人独立 Key 和数据空间；新增 api_key 表，Schedule/Todo 按 Key 隔离；各端增加登录/登出流程；新增 /api/auth/verify 验证接口 |
| v0.6 | 新增考试记录（不关联课程、自动关联待办）、循环待办规则（含 Rhino 自定义脚本）、AI 多步骤提案逐条确认 + 工作空间保护；Web 端响应式布局、字体大小设置、用户手册页、作息时间表仅管理员可编辑；考试移除 course_id |
