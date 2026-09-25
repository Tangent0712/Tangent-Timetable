export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface AuthVerifyResponse {
  valid: boolean
  label: string
  avatarUrl: string | null
}

export interface Schedule {
  id: number
  apiKey?: string
  name: string
  periodStartDate: string
  periodEndDate: string
  createdAt?: string
  updatedAt?: string
}

export interface Course {
  id: number
  scheduleId: number
  name: string
  location: string | null
  teacher: string | null
  dayOfWeek: number
  startPeriod: number
  endPeriod: number
  weeks: number[]
  createdAt?: string
  updatedAt?: string
}

export type CoursePayload = Omit<Course, 'id' | 'scheduleId' | 'createdAt' | 'updatedAt'>

/** 考试记录：不按课时，直接输入起止时间，展示为独立红色块 */
export interface Exam {
  id: number
  scheduleId: number
  name: string
  location: string | null
  examDate: string
  startTime: string
  endTime: string
  createdAt?: string
  updatedAt?: string
}

export type ExamPayload = Omit<Exam, 'id' | 'scheduleId' | 'createdAt' | 'updatedAt'>

export interface Todo {
  id: number
  title: string
  ddl: string
  completed: boolean
  /** 非空表示由循环规则自动生成 */
  recurringId: number | null
  /** 非空表示由考试自动关联生成（ddl=考试开始时间，考试结束后自动完成） */
  examId: number | null
  createdAt?: string
  updatedAt?: string
}

export type RecurringFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM'

export interface RecurringTodo {
  id: number
  title: string
  frequency: RecurringFrequency
  dayOfWeek: number | null
  dayOfMonth: number | null
  triggerTime: string | null
  /** CUSTOM 模式下的用户脚本，需定义 shouldTrigger(ctx) 返回布尔值 */
  script: string | null
  ddlOffsetMinutes: number
  enabled: boolean
  chainAfterComplete: boolean
  lastTriggeredAt: string | null
  nextTriggerAt: string | null
}

export type RecurringTodoPayload = Omit<RecurringTodo, 'id' | 'lastTriggeredAt' | 'nextTriggerAt'>

/** GET /api/recurring-todos/script-template 的返回 */
export interface RecurringScriptTemplate {
  template: string
  maxLength: number
  context: Record<string, unknown>
}

/** POST /api/recurring-todos/test-script 的返回 */
export interface RecurringScriptTestResult {
  valid: boolean
  triggeredNow: boolean
  error: string | null
}

export type PeriodCategory = 'MORNING' | 'AFTERNOON' | 'EVENING'

export interface PeriodConfig {
  periodNumber: number
  startTime: string
  endTime: string
  category: PeriodCategory
}

export type AiActionType =
  | 'CREATE_COURSE'
  | 'UPDATE_COURSE'
  | 'DELETE_COURSE'
  | 'CREATE_TODO'
  | 'UPDATE_TODO'
  | 'DELETE_TODO'
  | 'TOGGLE_TODO'
  | 'CREATE_RECURRING'
  | 'UPDATE_RECURRING'
  | 'DELETE_RECURRING'
  | 'TOGGLE_RECURRING'
  | 'CREATE_EXAM'
  | 'UPDATE_EXAM'
  | 'DELETE_EXAM'

export type AiActionStatus = 'PENDING' | 'EXECUTED' | 'REJECTED' | 'STALE'

export interface AiAction {
  actionId: number
  type: AiActionType
  status: AiActionStatus
  scope: 'COURSE' | 'TODO'
  data: Record<string, unknown> | null
}

export interface AiMessage {
  messageId: number
  role: 'user' | 'assistant'
  text: string
  actions?: AiAction[]
  createdAt?: string
}

export interface AiConversation {
  id: number
  scheduleId: number | null
  title: string
  createdAt?: string
  updatedAt?: string
}

export interface ParsedCourse {
  name: string | null
  location: string | null
  teacher: string | null
  dayOfWeek: number | null
  startPeriod: number | null
  endPeriod: number | null
  weeks: number[]
}

export interface ParseHtmlResult {
  courses: ParsedCourse[]
  note: string
}
