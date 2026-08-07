import { request } from './client'
import { streamMessage } from './stream'
import type {
  AiConversation,
  AiMessage,
  AuthVerifyResponse,
  Course,
  CoursePayload,
  ParseHtmlResult,
  PeriodConfig,
  RecurringTodo,
  RecurringTodoPayload,
  Schedule,
  ScheduleDetail,
  Todo,
} from '../types'

export const authApi = {
  verify: (apiKey: string) =>
    request<AuthVerifyResponse>('/auth/verify', { apiKey }),
}

export const scheduleApi = {
  list: () => request<Schedule[]>('/schedules'),
  detail: (id: number) => request<ScheduleDetail>(`/schedules/${id}`),
  create: (body: Omit<Schedule, 'id'>) =>
    request<Schedule>('/schedules', { method: 'POST', body }),
  update: (id: number, body: Omit<Schedule, 'id'>) =>
    request<Schedule>(`/schedules/${id}`, { method: 'PUT', body }),
  remove: (id: number) => request<null>(`/schedules/${id}`, { method: 'DELETE' }),
}

export const courseApi = {
  listBySchedule: (scheduleId: number) =>
    request<Course[]>(`/schedules/${scheduleId}/courses`),
  create: (scheduleId: number, body: CoursePayload) =>
    request<Course>(`/schedules/${scheduleId}/courses`, { method: 'POST', body }),
  batchCreate: (scheduleId: number, courses: CoursePayload[]) =>
    request<Course[]>(`/schedules/${scheduleId}/courses/batch`, {
      method: 'POST',
      body: { courses },
    }),
  update: (id: number, body: CoursePayload) =>
    request<Course>(`/courses/${id}`, { method: 'PUT', body }),
  remove: (id: number) => request<null>(`/courses/${id}`, { method: 'DELETE' }),
}

export const todoApi = {
  list: () => request<Todo[]>('/todos'),
  create: (body: { title: string; ddl: string }) =>
    request<Todo>('/todos', { method: 'POST', body }),
  update: (id: number, body: { title: string; ddl: string }) =>
    request<Todo>(`/todos/${id}`, { method: 'PUT', body }),
  remove: (id: number) => request<null>(`/todos/${id}`, { method: 'DELETE' }),
  toggle: (id: number) => request<Todo>(`/todos/${id}/toggle`, { method: 'PUT' }),
}

export const recurringApi = {
  list: () => request<RecurringTodo[]>('/recurring-todos'),
  create: (body: RecurringTodoPayload) =>
    request<RecurringTodo>('/recurring-todos', { method: 'POST', body }),
  update: (id: number, body: RecurringTodoPayload) =>
    request<RecurringTodo>(`/recurring-todos/${id}`, { method: 'PUT', body }),
  remove: (id: number) =>
    request<null>(`/recurring-todos/${id}`, { method: 'DELETE' }),
  toggle: (id: number) =>
    request<RecurringTodo>(`/recurring-todos/${id}/toggle`, { method: 'PUT' }),
  triggerNow: (id: number) =>
    request<RecurringTodo>(`/recurring-todos/${id}/trigger`, { method: 'POST' }),
}

export const periodApi = {
  list: () => request<PeriodConfig[]>('/period-config'),
  update: (configs: PeriodConfig[]) =>
    request<PeriodConfig[]>('/period-config', { method: 'PUT', body: { configs } }),
}

export const aiApi = {
  status: () => request<{ enabled: boolean }>('/ai/status'),
  listConversations: () => request<AiConversation[]>('/ai/conversations'),
  createConversation: (scheduleId: number | null) =>
    request<AiConversation>('/ai/conversations', {
      method: 'POST',
      body: { scheduleId },
    }),
  deleteConversation: (id: number) =>
    request<null>(`/ai/conversations/${id}`, { method: 'DELETE' }),
  renameConversation: (id: number, title: string) =>
    request<AiConversation>(`/ai/conversations/${id}`, {
      method: 'PUT',
      body: { title },
    }),
  listMessages: (id: number) =>
    request<AiMessage[]>(`/ai/conversations/${id}/messages`),
  sendMessage: (id: number, content: string, scheduleId: number | null) =>
    request<AiMessage>(`/ai/conversations/${id}/messages`, {
      method: 'POST',
      body: { content, scheduleId },
    }),
  sendMessageStream: streamMessage,
  execute: (id: number, actionId: number) =>
    request<AiMessage>(`/ai/conversations/${id}/actions/${actionId}/execute`, {
      method: 'POST',
    }),
  reject: (id: number, actionId: number) =>
    request<AiMessage>(`/ai/conversations/${id}/actions/${actionId}/reject`, {
      method: 'POST',
    }),
  parseHtml: (html: string, scheduleId: number | null) =>
    request<ParseHtmlResult>(
      `/ai/parse-html${scheduleId ? `?scheduleId=${scheduleId}` : ''}`,
      { method: 'POST', body: { html } },
    ),
}
