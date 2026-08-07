import { useState } from 'react'
import { Alert, Button, Card, Form, Input, Typography } from 'antd'
import { KeyOutlined } from '@ant-design/icons'
import { useApp } from '../store/AppContext'

export default function LoginPage() {
  const { login } = useApp()
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
      <Card className="login-card">
        <Typography.Title level={3} style={{ marginTop: 0 }}>
          TimeTable
        </Typography.Title>
        <Typography.Paragraph type="secondary">
          输入管理员分配给你的 API Key 以进入你的课程表与待办空间。
        </Typography.Paragraph>
        {error && (
          <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />
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
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
