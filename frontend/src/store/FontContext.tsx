import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'

const GLOBAL_KEY = 'timetable.fontScaleGlobal'
const TT_KEY = 'timetable.fontScaleTimetable'
const DEFAULT_SCALE = 1

interface FontCtx {
  globalScale: number
  ttScale: number
  setGlobalScale: (v: number) => void
  setTtScale: (v: number) => void
}

const Ctx = createContext<FontCtx>({
  globalScale: DEFAULT_SCALE,
  ttScale: DEFAULT_SCALE,
  setGlobalScale: () => {},
  setTtScale: () => {},
})

export const useFont = () => useContext(Ctx)

function load(key: string): number {
  const v = Number(localStorage.getItem(key))
  return Number.isFinite(v) && v > 0 ? v : DEFAULT_SCALE
}

/**
 * 字体大小设置。分两档：
 * - globalScale：全局其它字体（antd 组件 + body），写 CSS 变量 --app-font-scale
 * - ttScale：课程表部分字体，写 CSS 变量 --tt-scale（配合 em 实现整体缩放）
 * 数据保存在当前设备的 localStorage。
 */
export function FontProvider({ children }: { children: ReactNode }) {
  const [globalScale, setGlobalScaleState] = useState(() => load(GLOBAL_KEY))
  const [ttScale, setTtScaleState] = useState(() => load(TT_KEY))

  useEffect(() => {
    document.documentElement.style.setProperty('--app-font-scale', String(globalScale))
  }, [globalScale])

  useEffect(() => {
    document.documentElement.style.setProperty('--tt-scale', String(ttScale))
  }, [ttScale])

  const setGlobalScale = (v: number) => {
    setGlobalScaleState(v)
    localStorage.setItem(GLOBAL_KEY, String(v))
  }
  const setTtScale = (v: number) => {
    setTtScaleState(v)
    localStorage.setItem(TT_KEY, String(v))
  }

  return (
    <Ctx.Provider value={{ globalScale, ttScale, setGlobalScale, setTtScale }}>
      {children}
    </Ctx.Provider>
  )
}
