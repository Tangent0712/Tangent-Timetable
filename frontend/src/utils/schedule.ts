import dayjs, { type Dayjs } from 'dayjs'
import isoWeek from 'dayjs/plugin/isoWeek'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(isoWeek)
dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

export const WEEK_DAY_NAMES = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export function weekDayName(dayOfWeek: number): string {
  return WEEK_DAY_NAMES[dayOfWeek - 1] ?? '—'
}

/** 学期第 1 周为开始日期所在的那个 ISO 周 */
export function totalWeeks(start: string, end: string): number {
  const firstMonday = dayjs(start).startOf('isoWeek')
  const diff = dayjs(end).diff(firstMonday, 'day')
  return Math.max(1, Math.floor(diff / 7) + 1)
}

export function currentWeekOf(start: string, date: Dayjs = dayjs()): number {
  const firstMonday = dayjs(start).startOf('isoWeek')
  const diff = date.startOf('day').diff(firstMonday, 'day')
  return Math.floor(diff / 7) + 1
}

export function mondayOfWeek(start: string, week: number): Dayjs {
  return dayjs(start)
    .startOf('isoWeek')
    .add(week - 1, 'week')
}

export function dateOfWeekDay(start: string, week: number, dayOfWeek: number): Dayjs {
  return mondayOfWeek(start, week).add(dayOfWeek - 1, 'day')
}

/** 学期周次描述，处理未开始/已结束的边界情况 */
export function formatWeekInfo(start: string, end: string): string {
  const total = totalWeeks(start, end)
  const current = currentWeekOf(start)
  if (current < 1) return `学期未开始 · 共 ${total} 周`
  if (current > total) return `学期已结束 · 共 ${total} 周`
  return `第 ${current} / ${total} 周`
}

/** 两元素倒计时格式：1天4小时 / 4小时56分钟 / 7分钟45秒 */
export function formatCountdown(target: string, now: Dayjs = dayjs()): string {
  const diff = dayjs(target).diff(now, 'second')
  if (diff <= 0) return '已过期'

  const days = Math.floor(diff / 86400)
  const hours = Math.floor((diff % 86400) / 3600)
  const minutes = Math.floor((diff % 3600) / 60)
  const seconds = diff % 60

  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时${minutes}分钟`
  if (minutes > 0) return `${minutes}分钟${seconds}秒`
  return `${seconds}秒`
}

/** 把周次数组压缩为可读区间：[1,2,3,5,7,9] → "1-3, 5, 7, 9" */
export function formatWeeks(weeks: number[]): string {
  if (!weeks || weeks.length === 0) return '—'
  const sorted = [...weeks].sort((a, b) => a - b)
  const parts: string[] = []
  let rangeStart = sorted[0]
  let prev = sorted[0]

  for (let i = 1; i <= sorted.length; i++) {
    const cur = sorted[i]
    if (cur !== prev + 1) {
      parts.push(rangeStart === prev ? `${rangeStart}` : `${rangeStart}-${prev}`)
      rangeStart = cur
    }
    prev = cur
  }
  return parts.join(', ')
}

export function parseWeeksInput(input: string): number[] {
  const result = new Set<number>()
  input
    .split(/[,，\s]+/)
    .filter(Boolean)
    .forEach((token) => {
      const range = token.match(/^(\d+)\s*[-~]\s*(\d+)$/)
      if (range) {
        const from = Number(range[1])
        const to = Number(range[2])
        for (let i = Math.min(from, to); i <= Math.max(from, to); i++) result.add(i)
      } else if (/^\d+$/.test(token)) {
        result.add(Number(token))
      }
    })
  return [...result].sort((a, b) => a - b)
}

export function rangeWeeks(count: number): number[] {
  return Array.from({ length: count }, (_, i) => i + 1)
}

export { dayjs }
