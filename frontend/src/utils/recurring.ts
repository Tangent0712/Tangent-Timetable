import type { RecurringTodo } from '../types'
import { weekDayName } from './schedule'

/** 把分钟数转成「2 天」「4 小时30 分钟」这样的可读描述 */
export function describeOffset(minutes: number): string {
  if (!minutes || minutes <= 0) return '0 分钟'
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  const mins = minutes % 60
  const parts: string[] = []
  if (days > 0) parts.push(`${days} 天`)
  if (hours > 0) parts.push(`${hours} 小时`)
  if (mins > 0) parts.push(`${mins} 分钟`)
  return parts.join('')
}

/** 循环频率的可读描述，如「每周五 12:00」 */
export function describeFrequency(rule: RecurringTodo): string {
  const time = rule.triggerTime?.slice(0, 5) ?? '--:--'
  switch (rule.frequency) {
    case 'DAILY':
      return `每天 ${time}`
    case 'WEEKLY':
      return `每${weekDayName(rule.dayOfWeek ?? 0).replace('周', '周')} ${time}`
    case 'MONTHLY':
      return `每月 ${rule.dayOfMonth} 号 ${time}`
    default:
      return time
  }
}

export const FREQUENCY_LABELS: Record<string, string> = {
  DAILY: '每天',
  WEEKLY: '每周',
  MONTHLY: '每月',
}
