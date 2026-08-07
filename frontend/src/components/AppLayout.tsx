import { useMemo } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  Button,
  Dropdown,
  Layout,
  Menu,
  Select,
  Space,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  CalendarOutlined,
  CheckSquareOutlined,
  LogoutOutlined,
  MessageOutlined,
  ReloadOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import type { ReactNode } from 'react'
import { useApp } from '../store/AppContext'
import { formatWeekInfo } from '../utils/schedule'

const { Header, Content } = Layout

const NAV_ITEMS = [
  { key: '/timetable', icon: <CalendarOutlined />, label: <Link to="/timetable">课表</Link> },
  { key: '/todos', icon: <CheckSquareOutlined />, label: <Link to="/todos">待办</Link> },
  { key: '/ai', icon: <MessageOutlined />, label: <Link to="/ai">AI 助手</Link> },
  { key: '/settings', icon: <SettingOutlined />, label: <Link to="/settings">设置</Link> },
]

export default function AppLayout({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const {
    label,
    logout,
    schedules,
    activeScheduleId,
    activeSchedule,
    setActiveScheduleId,
    refreshAll,
    loading,
  } = useApp()

  const weekInfo = useMemo(() => {
    if (!activeSchedule) return null
    return formatWeekInfo(activeSchedule.periodStartDate, activeSchedule.periodEndDate)
  }, [activeSchedule])

  const selectedKey =
    NAV_ITEMS.find((item) => location.pathname.startsWith(item.key))?.key ?? '/timetable'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          paddingInline: 20,
        }}
      >
        <Typography.Text
          strong
          style={{ fontSize: 18, cursor: 'pointer', whiteSpace: 'nowrap' }}
          onClick={() => navigate('/timetable')}
        >
          TimeTable
        </Typography.Text>

        <Menu
          mode="horizontal"
          selectedKeys={[selectedKey]}
          items={NAV_ITEMS}
          style={{ flex: 1, minWidth: 320, borderBottom: 'none' }}
        />

        <Space size={8}>
          <Select
            value={activeScheduleId ?? undefined}
            onChange={(v) => setActiveScheduleId(v)}
            placeholder="选择课表"
            style={{ minWidth: 180 }}
            options={schedules.map((s) => ({ value: s.id, label: s.name }))}
            notFoundContent="暂无课表"
          />
          {weekInfo && <Tag color="blue">{weekInfo}</Tag>}
          <Tooltip title="立即同步">
            <Button
              icon={<ReloadOutlined />}
              loading={loading}
              onClick={() => refreshAll()}
            />
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
            <Button icon={<UserOutlined />}>{label || '我'}</Button>
          </Dropdown>
        </Space>
      </Header>

      <Content style={{ padding: 20 }}>{children}</Content>
    </Layout>
  )
}
