import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { message } from 'antd'
import {
  clearApiKey,
  getApiKey,
  getLabel,
  onUnauthorized,
  setApiKey as persistApiKey,
} from '../api/client'
import { authApi, courseApi, periodApi, recurringApi, scheduleApi, todoApi } from '../api'
import type { Course, PeriodConfig, RecurringTodo, Schedule, Todo } from '../types'

const ACTIVE_SCHEDULE_STORAGE = 'timetable.activeScheduleId'
const SYNC_INTERVAL_MS = 30_000

interface AppState {
  ready: boolean
  authed: boolean
  label: string
  schedules: Schedule[]
  activeScheduleId: number | null
  activeSchedule: Schedule | null
  courses: Course[]
  todos: Todo[]
  recurringTodos: RecurringTodo[]
  periods: PeriodConfig[]
  loading: boolean
  login: (key: string) => Promise<void>
  logout: () => void
  setActiveScheduleId: (id: number | null) => void
  refreshSchedules: () => Promise<Schedule[]>
  refreshCourses: () => Promise<void>
  refreshTodos: () => Promise<void>
  refreshRecurring: () => Promise<void>
  refreshPeriods: () => Promise<void>
  refreshAll: () => Promise<void>
}

const AppContext = createContext<AppState | null>(null)

export function AppProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false)
  const [authed, setAuthed] = useState(false)
  const [label, setLabel] = useState(getLabel())
  const [schedules, setSchedules] = useState<Schedule[]>([])
  const [activeScheduleId, setActiveScheduleIdState] = useState<number | null>(() => {
    const stored = localStorage.getItem(ACTIVE_SCHEDULE_STORAGE)
    return stored ? Number(stored) : null
  })
  const [courses, setCourses] = useState<Course[]>([])
  const [todos, setTodos] = useState<Todo[]>([])
  const [recurringTodos, setRecurringTodos] = useState<RecurringTodo[]>([])
  const [periods, setPeriods] = useState<PeriodConfig[]>([])
  const [loading, setLoading] = useState(false)

  const activeIdRef = useRef(activeScheduleId)
  activeIdRef.current = activeScheduleId

  const setActiveScheduleId = useCallback((id: number | null) => {
    setActiveScheduleIdState(id)
    if (id === null) localStorage.removeItem(ACTIVE_SCHEDULE_STORAGE)
    else localStorage.setItem(ACTIVE_SCHEDULE_STORAGE, String(id))
  }, [])

  const doLogout = useCallback(() => {
    clearApiKey()
    setAuthed(false)
    setLabel('')
    setSchedules([])
    setCourses([])
    setTodos([])
    setRecurringTodos([])
    setPeriods([])
    setActiveScheduleId(null)
  }, [setActiveScheduleId])

  useEffect(() => {
    onUnauthorized(() => {
      doLogout()
      message.error('API Key 已失效，请重新登录')
    })
  }, [doLogout])

  const refreshSchedules = useCallback(async () => {
    const list = await scheduleApi.list()
    setSchedules(list)
    const currentId = activeIdRef.current
    if (list.length === 0) {
      setActiveScheduleId(null)
    } else if (currentId === null || !list.some((s) => s.id === currentId)) {
      setActiveScheduleId(list[0].id)
    }
    return list
  }, [setActiveScheduleId])

  const refreshCourses = useCallback(async () => {
    const id = activeIdRef.current
    if (id === null) {
      setCourses([])
      return
    }
    setCourses(await courseApi.listBySchedule(id))
  }, [])

  const refreshTodos = useCallback(async () => {
    setTodos(await todoApi.list())
  }, [])

  const refreshRecurring = useCallback(async () => {
    setRecurringTodos(await recurringApi.list())
  }, [])

  const refreshPeriods = useCallback(async () => {
    setPeriods(await periodApi.list())
  }, [])

  const refreshAll = useCallback(async () => {
    setLoading(true)
    try {
      await refreshSchedules()
      await Promise.all([
        refreshCourses(),
        refreshTodos(),
        refreshRecurring(),
        refreshPeriods(),
      ])
    } finally {
      setLoading(false)
    }
  }, [refreshCourses, refreshPeriods, refreshRecurring, refreshSchedules, refreshTodos])

  const login = useCallback(
    async (key: string) => {
      const result = await authApi.verify(key)
      if (!result.valid) throw new Error('API Key 无效')
      persistApiKey(key, result.label)
      setLabel(result.label)
      setAuthed(true)
    },
    [],
  )

  useEffect(() => {
    const key = getApiKey()
    if (!key) {
      setReady(true)
      return
    }
    authApi
      .verify(key)
      .then((res) => {
        if (res.valid) {
          setAuthed(true)
          setLabel(res.label)
        } else {
          clearApiKey()
        }
      })
      .catch(() => clearApiKey())
      .finally(() => setReady(true))
  }, [])

  useEffect(() => {
    if (!authed) return
    refreshAll().catch((e: Error) => message.error(e.message))
  }, [authed, refreshAll])

  useEffect(() => {
    if (!authed) return
    refreshCourses().catch((e: Error) => message.error(e.message))
  }, [authed, activeScheduleId, refreshCourses])

  useEffect(() => {
    if (!authed) return
    const timer = window.setInterval(() => {
      Promise.all([refreshCourses(), refreshTodos(), refreshRecurring()]).catch(() => {
        /* 静默失败，下次轮询重试 */
      })
    }, SYNC_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [authed, refreshCourses, refreshTodos, refreshRecurring])

  const activeSchedule = useMemo(
    () => schedules.find((s) => s.id === activeScheduleId) ?? null,
    [schedules, activeScheduleId],
  )

  const value: AppState = {
    ready,
    authed,
    label,
    schedules,
    activeScheduleId,
    activeSchedule,
    courses,
    todos,
    recurringTodos,
    periods,
    loading,
    login,
    logout: doLogout,
    setActiveScheduleId,
    refreshSchedules,
    refreshCourses,
    refreshTodos,
    refreshRecurring,
    refreshPeriods,
    refreshAll,
  }

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>
}

export function useApp(): AppState {
  const ctx = useContext(AppContext)
  if (!ctx) throw new Error('useApp must be used within AppProvider')
  return ctx
}
