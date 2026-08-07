import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { theme as antdTheme, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'

type Mode = 'light' | 'dark'

interface ThemeCtx {
  mode: Mode
  toggle: () => void
}

const Ctx = createContext<ThemeCtx>({ mode: 'light', toggle: () => {} })
export const useTheme = () => useContext(Ctx)

const STORAGE_KEY = 'timetable-theme'

function getInitialMode(): Mode {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

const TOKENS = {
  light: {
    colorPrimary: '#f8c3cd',
    colorBgBase: '#f5f0eb',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#fafaf7',
    colorBgLayout: '#f5f0eb',
    colorText: '#2d2d2d',
    colorTextSecondary: '#888888',
    colorBorder: '#2d2d2d',
    colorBorderSecondary: '#dddddd',
    colorSplit: '#d0d0d0',
    borderRadius: 4,
    fontFamily: "'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Consolas, monospace",
    fontSize: 13,
    controlHeight: 32,
  },
  dark: {
    colorPrimary: '#9575cd',
    colorBgBase: '#1a1a1a',
    colorBgContainer: '#242424',
    colorBgElevated: '#363636',
    colorBgLayout: '#1a1a1a',
    colorText: '#f0ebe4',
    colorTextSecondary: '#999999',
    colorBorder: '#4a4a4a',
    colorBorderSecondary: '#4a4a4a',
    colorSplit: '#4a4a4a',
    borderRadius: 4,
    fontFamily: "'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Consolas, monospace",
    fontSize: 13,
    controlHeight: 32,
  },
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [mode, setMode] = useState<Mode>(getInitialMode)

  useEffect(() => {
    document.documentElement.dataset.theme = mode
    localStorage.setItem(STORAGE_KEY, mode)
  }, [mode])

  const toggle = () => setMode((m) => (m === 'light' ? 'dark' : 'light'))

  return (
    <Ctx.Provider value={{ mode, toggle }}>
      <ConfigProvider
        locale={zhCN}
        theme={{
          algorithm: mode === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
          token: TOKENS[mode],
          components: {
            Layout: {
              headerBg: mode === 'dark' ? '#2d2d2d' : '#2d2d2d',
              siderBg: mode === 'dark' ? '#363636' : '#fafaf7',
              bodyBg: mode === 'dark' ? '#1a1a1a' : '#f5f0eb',
              footerBg: mode === 'dark' ? '#111111' : '#f0ebe4',
            },
            Menu: {
              itemBg: 'transparent',
              itemColor: mode === 'dark' ? '#f0ebe4' : '#2d2d2d',
              itemSelectedBg: mode === 'dark' ? '#9575cd' : '#f8c3cd',
              itemSelectedColor: '#2d2d2d',
              itemBorderRadius: 4,
              itemHeight: 34,
              itemMarginInline: 0,
              activeBarHeight: 0,
              activeBarBorderWidth: 0,
              subMenuItemBg: 'transparent',
            },
            Card: {
              headerBg: mode === 'dark' ? '#363636' : '#fafaf7',
              headerFontSize: 14,
            },
            Modal: {
              headerBg: mode === 'dark' ? '#242424' : '#ffffff',
              contentBg: mode === 'dark' ? '#242424' : '#ffffff',
            },
            Table: {
              headerBg: mode === 'dark' ? '#363636' : '#fafaf7',
              headerColor: mode === 'dark' ? '#f0ebe4' : '#2d2d2d',
              rowHoverBg: mode === 'dark' ? '#2d2d2d' : '#fafaf7',
              borderColor: mode === 'dark' ? '#4a4a4a' : '#dddddd',
            },
            Tag: {
              defaultBg: mode === 'dark' ? '#363636' : '#fafaf7',
              defaultColor: mode === 'dark' ? '#f0ebe4' : '#2d2d2d',
            },
          },
        }}
      >
        {children}
      </ConfigProvider>
    </Ctx.Provider>
  )
}
