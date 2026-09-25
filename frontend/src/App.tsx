import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import AppLayout from './components/AppLayout'
import LoginPage from './pages/LoginPage'
import TimetablePage from './pages/TimetablePage'
import TodosPage from './pages/TodosPage'
import AiChatPage from './pages/AiChatPage'
import SettingsPage from './pages/SettingsPage'
import UserManualPage from './pages/UserManualPage'
import WidgetPreviewPage from './pages/WidgetPreviewPage'
import { useApp } from './store/AppContext'

export default function App() {
  const { ready, authed } = useApp()
  const location = useLocation()

  if (location.pathname.startsWith('/widget')) {
    return <WidgetPreviewPage />
  }

  if (!ready) {
    return (
      <div className="app-loading">
        <Spin size="large" />
      </div>
    )
  }

  if (!authed) {
    return <LoginPage />
  }

  return (
    <AppLayout>
      <Routes>
        <Route path="/timetable" element={<TimetablePage />} />
        <Route path="/todos" element={<TodosPage />} />
        <Route path="/ai" element={<AiChatPage />} />
        <Route path="/manual" element={<UserManualPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/timetable" replace />} />
      </Routes>
    </AppLayout>
  )
}
