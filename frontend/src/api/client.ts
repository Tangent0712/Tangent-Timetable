import type { ApiResponse } from '../types'

const API_KEY_STORAGE = 'timetable.apiKey'
const API_LABEL_STORAGE = 'timetable.apiLabel'

export const BASE_URL = import.meta.env.VITE_API_BASE ?? '/api'

export function getApiKey(): string | null {
  return localStorage.getItem(API_KEY_STORAGE)
}

export function setApiKey(key: string, label: string) {
  localStorage.setItem(API_KEY_STORAGE, key)
  localStorage.setItem(API_LABEL_STORAGE, label)
}

export function getLabel(): string {
  return localStorage.getItem(API_LABEL_STORAGE) ?? ''
}

export function clearApiKey() {
  localStorage.removeItem(API_KEY_STORAGE)
  localStorage.removeItem(API_LABEL_STORAGE)
}

export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

type Options = {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  apiKey?: string
}

let unauthorizedHandler: (() => void) | null = null

export function onUnauthorized(handler: () => void) {
  unauthorizedHandler = handler
}

export async function request<T>(path: string, options: Options = {}): Promise<T> {
  const { method = 'GET', body, apiKey } = options
  const key = apiKey ?? getApiKey()

  const headers: Record<string, string> = {}
  if (key) headers['X-API-Key'] = key
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let res: Response
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, '网络请求失败，请检查后端服务是否启动')
  }

  let payload: ApiResponse<T> | null = null
  const text = await res.text()
  if (text) {
    try {
      payload = JSON.parse(text) as ApiResponse<T>
    } catch {
      payload = null
    }
  }

  if (payload === null) {
    throw new ApiError(res.status, `请求失败 (HTTP ${res.status})`)
  }

  if (payload.code !== 200) {
    if (payload.code === 401 && unauthorizedHandler) {
      unauthorizedHandler()
    }
    throw new ApiError(payload.code, payload.message || '请求失败')
  }

  return payload.data
}
