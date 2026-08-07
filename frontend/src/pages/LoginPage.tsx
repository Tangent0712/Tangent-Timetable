import { useState } from 'react'
import { Alert, Button, Form, Input, Typography } from 'antd'
import { KeyOutlined } from '@ant-design/icons'
import { useApp } from '../store/AppContext'
import { useTheme } from '../theme'

export default function LoginPage() {
  const { login } = useApp()
  const { mode, toggle } = useTheme()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const onFinish = async ({ apiKey }: { apiKey: string }) => {
    setLoading(true)
    setError(null)
    try {
      await login(apiKey.trim())
    } catch (e) {
      setError(e instanceof Error ? e.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div
          className="login-card-header"
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
        >
          <span>大切课程表</span>
          <div
            className="icon-btn-round"
            onClick={toggle}
            style={{ width: 28, height: 28, fontSize: 14 }}
          >
            {mode === 'dark' ? '☀' : '☾'}
          </div>
        </div>
        <div className="login-card-body">
          <Typography.Paragraph
            type="secondary"
            style={{ fontSize: 12, marginBottom: 16 }}
          >
            {'> 输入管理员分配给你的 API Key 以进入你的课程表与待办空间。'}
          </Typography.Paragraph>
          {error && (
            <Alert
              type="error"
              message={error}
              showIcon
              style={{ marginBottom: 16 }}
            />
          )}
          <Form layout="vertical" onFinish={onFinish} requiredMark={false}>
            <Form.Item
              name="apiKey"
              label="API Key"
              rules={[{ required: true, message: '请输入 API Key' }]}
            >
              <Input
                prefix={<KeyOutlined />}
                placeholder="例如 demo-key-001"
                autoFocus
                allowClear
              />
            </Form.Item>
            <Form.Item style={{ marginBottom: 0 }}>
              <Button type="primary" htmlType="submit" block loading={loading}>
                {'> 登录'}
              </Button>
            </Form.Item>
          </Form>
          <div style={{ textAlign: 'center', marginTop: 16 }}>
            <Typography.Text
              type="secondary"
              style={{ fontSize: 9, letterSpacing: 0.5 }}
            >
              ૮₍◜ෆ◝.₎ა © 2026 大切课程表 ˶ˊᜊˋ˶
            </Typography.Text>
          </div>
        </div>
      </div>
    </div>
  )
}
