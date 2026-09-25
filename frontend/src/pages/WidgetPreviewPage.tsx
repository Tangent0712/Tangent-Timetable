import { useCallback, useEffect, useRef, useState } from 'react'
import { Spin } from 'antd'
import { courseColor } from '../utils/color'
import { BASE_URL, getApiKey } from '../api/client'

type Schedule = { id: number; name: string }

type CourseItem = {
  name?: string
  time?: string
  location?: string
  teacher?: string
  period?: string
  status?: string
  date?: string
  week?: number
  dow?: string
  sortKey?: number
}
type TodoItem = { title?: string; ddlText?: string; ddlRemain?: string; overdue?: boolean }
type ExamItem = {
  name?: string
  time?: string
  location?: string
  date?: string
  week?: number
  dow?: string
  status?: string
  sortKey?: number
}
type Item = {
  kind: 'course' | 'exam'
  sortKey: number
  name?: string
  time?: string
  location?: string
  teacher?: string
  period?: string
  status?: string
  date?: string
  week?: number
  dow?: string
}

type WidgetData = {
  title?: string
  updatedAt?: string
  week?: string
  today?: string
  todayCourses?: CourseItem[]
  todayRemaining?: number
  todos?: TodoItem[]
  todoSummary?: string
  exams?: ExamItem[]
}

const REFRESH_MS = 60 * 1000

function parseQuery(): { key?: string; scheduleId?: number } {
  const params = new URLSearchParams(window.location.search)
  const key = params.get('key')?.trim() || undefined
  const sidRaw = params.get('scheduleId')
  const scheduleId = sidRaw ? Number(sidRaw) : undefined
  return { key, scheduleId: Number.isFinite(scheduleId) ? scheduleId : undefined }
}

export default function WidgetPreviewPage() {
  const [{ key, scheduleId }, setQuery] = useState(parseQuery)
  const [schedules, setSchedules] = useState<Schedule[]>([])
  const [data, setData] = useState<WidgetData | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const timer = useRef<number>()

  const apiKey = key || getApiKey() || 'demo-key-001'

  // 拉取该 key 下的课表列表，供选择
  const loadSchedules = useCallback(async (k: string) => {
    try {
      const res = await fetch(`${BASE_URL}/schedules`, {
        headers: { 'X-API-Key': k },
      })
      const payload = await res.json()
      if (payload.code === 200) setSchedules(payload.data ?? [])
    } catch {
      /* 忽略，不影响主流程 */
    }
  }, [])

  const load = useCallback(async (k: string, sid?: number) => {
    setLoading(true)
    setError('')
    try {
      const path = `/widget/overview/${encodeURIComponent(k)}${sid ? `?scheduleId=${sid}` : ''}`
      const res = await fetch(`${BASE_URL}${path}`)
      const payload = await res.json()
      if (payload.code !== 200) throw new Error(payload.message || '请求失败')
      setData(payload.data)
    } catch (e) {
      const msg = (e as Error).message || '未知错误'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadSchedules(apiKey)
  }, [apiKey, loadSchedules])

  useEffect(() => {
    load(apiKey, scheduleId)
    timer.current = window.setInterval(() => load(apiKey, scheduleId), REFRESH_MS)
    const onVisible = () => {
      if (document.visibilityState === 'visible') load(apiKey, scheduleId)
    }
    document.addEventListener('visibilitychange', onVisible)
    window.addEventListener('focus', onVisible)
    return () => {
      window.clearInterval(timer.current)
      document.removeEventListener('visibilitychange', onVisible)
      window.removeEventListener('focus', onVisible)
    }
  }, [apiKey, scheduleId, load])

  // 未指定课表时，默认取第一个
  useEffect(() => {
    if (scheduleId === undefined && schedules.length > 0) {
      setQuery((prev) => ({ ...prev, scheduleId: schedules[0].id }))
    }
  }, [scheduleId, schedules])

  const courses = data?.todayCourses ?? []
  const exams = (data?.exams ?? []).slice(0, 2)
  const allItems: Item[] = [
    ...courses.map((c) => ({ ...c, kind: 'course' as const, sortKey: c.sortKey ?? 0 })),
    ...exams.map((e) => ({ ...e, kind: 'exam' as const, sortKey: e.sortKey ?? 0 })),
  ].sort((a, b) => a.sortKey - b.sortKey)

  // 全部渲染，靠容器 overflow:hidden 按可视高度响应式裁切
  const visibleItems = allItems.slice(0, 6)
  const todos = data?.todos ?? []

  return (
    <div className="widget-page">
      {loading && !data ? (
        <div className="widget-card-loading">
          <Spin />
        </div>
      ) : error && !data ? (
        <div className="widget-card-error">加载失败：{error}</div>
      ) : (
        <>
          <div className="widget-head">
            <span className="widget-title">{data?.title || '课程表'}</span>
            <span className="widget-meta">
              {data?.week ? `${data.week}·` : ''}
              {data?.today || ''}
              {data?.updatedAt ? `·更新 ${data.updatedAt}` : ''}
            </span>
          </div>

          <div className="widget-body">
            <div className="widget-col timetable">
              <div className="widget-col-title">
                <span>课程和考试</span>
                <span className="widget-col-count">今日剩余{data?.todayRemaining ?? 0}项</span>
              </div>
              <div className="widget-course-list">
                {visibleItems.length ? (
                  visibleItems.map((c, i) => {
                    if (c.kind === 'exam') {
                      return (
                        <div className="widget-exam-card" key={`exam-${i}`}>
                          <div className="widget-exam-card-tag">考</div>
                          <div className="widget-exam-info">
                            <div className="widget-exam-card-head-row">
                              <div className="widget-course-card-name">{c.name}</div>
                              <span className="widget-course-card-status" data-status={c.status}>
                                {c.status}
                              </span>
                            </div>
                            <div className="widget-course-card-meta">
                              {c.date}·第{c.week}周·{c.dow}
                            </div>
                          </div>
                        </div>
                      )
                    }
                    const color = courseColor(c.name || '')
                    return (
                      <div
                        className="widget-course-card"
                        key={`course-${i}`}
                        style={{
                          background: '#2d2d2d',
                          borderLeftColor: color.border,
                          color: '#f0ebe4',
                        }}
                      >
                        <div className="widget-course-card-head">
                          <span className="widget-course-card-name">{c.name}</span>
                          <span className="widget-course-card-status" data-status={c.status}>
                            {c.status}
                          </span>
                        </div>
                        <div className="widget-course-card-meta">
                          {c.date}·{c.dow}·第{c.week}周{c.location ? `·${c.location}` : ''}
                        </div>
                        <div className="widget-course-card-meta">
                          {c.period ? `${c.period}·` : ''}
                          {c.time}
                          {c.teacher ? `·${c.teacher}` : ''}
                        </div>
                      </div>
                    )
                  })
                ) : (
                  <div className="widget-empty">今天没有课</div>
                )}
              </div>
            </div>

            <div className="widget-col-divider" />

            <div className="widget-col todos">
              <div className="widget-col-title">
                <span>待办</span>
                <span className="widget-col-count">{data?.todoSummary || '暂无'}</span>
              </div>
              <div className="widget-todo-list">
                {todos.length ? (
                  todos.map((t, i) => (
                    <div className="widget-todo-row" key={i}>
                      <span className="widget-todo-dot" data-overdue={!!t.overdue}>
                        {t.overdue ? '●' : '○'}
                      </span>
                      <span className="widget-todo-title" data-overdue={!!t.overdue}>
                        {t.title}
                      </span>
                      <span className="widget-todo-meta">
                        <span className="widget-todo-ddl">{t.ddlText}</span>
                        <span className="widget-todo-remain" data-overdue={!!t.overdue}>
                          {t.ddlRemain || '-'}
                        </span>
                      </span>
                    </div>
                  ))
                ) : (
                  <div className="widget-empty">暂无待办</div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
