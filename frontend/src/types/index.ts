export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface AuthVerifyResponse {
  valid: boolean
  label: string
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

export interface Todo {
  id: number
  title: string
  ddl: string
  completed: boolean
  createdAt?: string
  updatedAt?: string
}

export type PeriodCategory = 'MORNING' | 'AFTERNOON' | 'EVENING'

export interface PeriodConfig {
  periodNumber: number
  startTime: string
  endTime: string
  category: PeriodCategory
}

export interface ScheduleDetail {
  schedule: Schedule
  courses: Course[]
}

export type AiActionType =
  | 'CREATE_COURSE'
  | 'UPDATE_COURSE'
  | 'DELETE_COURSE'
  | 'CREATE_TODO'
  | 'UPDATE_TODO'
  | 'DELETE_TODO'
  | 'TOGGLE_TODO'

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
