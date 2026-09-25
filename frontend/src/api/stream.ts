import { BASE_URL, getApiKey } from './client'
import type { AiMessage } from '../types'

export interface StreamHandlers {
  onThinking?: (delta: string) => void
  onText?: (delta: string) => void
  onPlanning?: () => void
}

/**
 * 通过 SSE 流式发送消息。
 * 事件序列：thinking* → text* → planning → done（出错时为 error）。
 */
export async function streamMessage(
  conversationId: number,
  content: string,
  scheduleId: number | null,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<AiMessage> {
  const key = getApiKey()
  const res = await fetch(`${BASE_URL}/ai/conversations/${conversationId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(key ? { 'X-API-Key': key } : {}),
    },
    body: JSON.stringify({ content, scheduleId }),
    signal,
  })

  if (!res.ok || !res.body) {
    throw new Error(`请求失败 (HTTP ${res.status})`)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let result: AiMessage | null = null
  let error: { code: number; message: string } | null = null

  // SSE 以空行分隔事件块，每块形如 "event: text\ndata: {...}"
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let sep: number
    while ((sep = buffer.indexOf('\n\n')) !== -1) {
      const block = buffer.slice(0, sep)
      buffer = buffer.slice(sep + 2)

      let event = 'message'
      const dataLines: string[] = []
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      if (dataLines.length === 0) continue

      let payload: unknown
      try {
        payload = JSON.parse(dataLines.join('\n'))
      } catch {
        continue
      }

      switch (event) {
        case 'thinking':
          handlers.onThinking?.((payload as { delta: string }).delta)
          break
        case 'text':
          handlers.onText?.((payload as { delta: string }).delta)
          break
        case 'planning':
          handlers.onPlanning?.()
          break
        case 'done':
          result = payload as AiMessage
          break
        case 'error':
          error = payload as { code: number; message: string }
          break
      }
    }
  }

  if (error) throw new Error(error.message)
  if (!result) throw new Error('AI 未返回结果')
  return result
}
