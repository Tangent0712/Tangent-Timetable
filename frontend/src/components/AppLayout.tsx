import { useMemo, type ReactNode } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Dropdown, Select, Tag, Tooltip } from 'antd'
import {
  CalendarOutlined,
  CheckSquareOutlined,
  LogoutOutlined,
  MessageOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { useApp } from '../store/AppContext'
import { useTheme } from '../theme'
import { formatWeekInfo } from '../utils/schedule'

const NAV_ITEMS = [
  { key: '/timetable', icon: <CalendarOutlined />, label: '课表' },
  { key: '/todos', icon: <CheckSquareOutlined />, label: '待办' },
  { key: '/ai', icon: <MessageOutlined />, label: 'AI 助手' },
  { key: '/settings', icon: <SettingOutlined />, label: '设置' },
]

export default function AppLayout({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { mode, toggle } = useTheme()
  const {
    label,
    avatarUrl,
    logout,
    schedules,
    activeScheduleId,
    activeSchedule,
    setActiveScheduleId,
    refreshAll,
    loading,
    todos,
    courses,
  } = useApp()

  const weekInfo = useMemo(() => {
    if (!activeSchedule) return null
    return formatWeekInfo(activeSchedule.periodStartDate, activeSchedule.periodEndDate)
  }, [activeSchedule])

  const selectedKey =
    NAV_ITEMS.find((item) => location.pathname.startsWith(item.key))?.key ?? '/timetable'

  const activeTodos = todos.filter((t) => !t.completed).length

  return (
    <div className="app-shell">
      {/* Top bar */}
      <div className="app-topbar">
        <div className="app-topbar-title" onClick={() => navigate('/timetable')}>
          大切课程表
        </div>
        <div className="app-topbar-actions">
          {weekInfo && (
            <Tag style={{ background: 'transparent', color: '#f0ebe4', border: '1px solid #888' }}>
              {weekInfo}
            </Tag>
          )}
          <Tooltip title="立即同步">
            <div
              className="icon-btn-round"
              onClick={() => refreshAll()}
              style={{ opacity: loading ? 0.5 : 1, pointerEvents: loading ? 'none' : 'auto' }}
            >
              <ReloadOutlined spin={loading} />
            </div>
          </Tooltip>
          <Tooltip title={mode === 'dark' ? '切换到亮色' : '切换到暗色'}>
            <div className="icon-btn-round" onClick={toggle}>
              {mode === 'dark' ? '☀' : '☾'}
            </div>
          </Tooltip>
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '登出',
                  danger: true,
                  onClick: logout,
                },
              ],
            }}
          >
            <div className="icon-btn-round" style={{ width: 'auto', padding: '0 12px', fontSize: 13 }}>
              {label || '我'}
            </div>
          </Dropdown>
        </div>
      </div>

      {/* Body: sidebar + content */}
      <div className="app-body">
        {/* Sidebar */}
        <aside className="app-sidebar">
          <div className="sidebar-avatar">
            <div className="sidebar-avatar-img">
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt="avatar"
                  style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
                />
              ) : (
                'T'
              )}
            </div>
            <div className="sidebar-avatar-name">{label || 'Tangent'}</div>
          </div>

          <div className="sidebar-divider" />

          <nav className="sidebar-nav">
            {NAV_ITEMS.map((item) => (
              <Link
                key={item.key}
                to={item.key}
                className={`sidebar-nav-item ${selectedKey === item.key ? 'active' : ''}`}
              >
                {selectedKey === item.key ? `✦ ${item.label}` : item.label}
              </Link>
            ))}
          </nav>

          <div className="sidebar-divider" />

          <div className="sidebar-stats">
            <div className="sidebar-stats-title">ฅ^•ﻌ•^ฅ 统计</div>
            <div className="sidebar-stats-item">课程: {courses.length} 门</div>
            <div className="sidebar-stats-item">待办: {activeTodos} 条</div>
            <div className="sidebar-stats-item">课表: {schedules.length} 个</div>
          </div>

          <div className="sidebar-divider" />

          {schedules.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              <div className="sidebar-stats-title">课表切换</div>
              <Select
                value={activeScheduleId ?? undefined}
                onChange={(v) => setActiveScheduleId(v)}
                placeholder="选择课表"
                style={{ width: '100%' }}
                options={schedules.map((s) => ({ value: s.id, label: s.name }))}
                notFoundContent="暂无课表"
                size="small"
              />
            </div>
          )}

          <div className="sidebar-divider" />

          <div className="sidebar-kaomoji">૮₍◜ෆ◝.₎ა</div>
        </aside>

        {/* Main content */}
        <main className="app-content">{children}</main>
      </div>

      {/* Footer */}
      <footer className="app-footer">
        <span className="app-footer-text">© 2026 大切课程表 | 正切的课程表</span>
        <span className="app-footer-text">˶ˊᜊˋ˶</span>
      </footer>
    </div>
  )
}
