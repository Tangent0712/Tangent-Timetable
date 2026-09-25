import { useEffect, useState } from 'react'
import { Button, DatePicker, Form, Input, message, Modal, Space } from 'antd'
import type { Dayjs } from 'dayjs'
import type { Schedule } from '../types'

interface Props {
  open: boolean
  onCancel: () => void
  onCreate: (payload: Omit<Schedule, 'id'>) => Promise<void>
}

interface FormValues {
  name: string
  range: [Dayjs, Dayjs]
}

// 只能选周一(1)或周日(0)，配合校验确保开始=周一、结束=周日
function semesterDisabledDate(current: Dayjs) {
  if (!current) return false
  const d = current.day()
  return d !== 0 && d !== 1
}

export default function ScheduleCreateModal({ open, onCancel, onCreate }: Props) {
  const [form] = Form.useForm<FormValues>()
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (open) form.resetFields()
  }, [open, form])

  const submit = async () => {
    const values = await form.validateFields()
    const payload = {
      name: values.name.trim(),
      periodStartDate: values.range[0].format('YYYY-MM-DD'),
      periodEndDate: values.range[1].format('YYYY-MM-DD'),
    }
    setSaving(true)
    try {
      await onCreate(payload)
      onCancel()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <Modal
      open={open}
      maskClosable={false}
      title="新建课表"
      onCancel={onCancel}
      footer={null}
      width={520}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item
          name="name"
          label="课表名称"
          rules={[{ required: true, message: '请输入课表名称' }]}
        >
          <Input placeholder="如 2025-2026 秋季学期" />
        </Form.Item>
        <Form.Item
          name="range"
          label="学期起止日期"
          rules={[
            { required: true, message: '请选择学期起止日期' },
            {
              validator: (_, value) => {
                if (!value) return Promise.resolve()
                const [start, end] = value as [Dayjs, Dayjs]
                if (!start || !end) return Promise.resolve()
                if (start.day() !== 1) return Promise.reject(new Error('开始日期必须是周一'))
                if (end.day() !== 0) return Promise.reject(new Error('结束日期必须是周日'))
                return Promise.resolve()
              },
            },
          ]}
          extra="学期开始必须是周一，结束必须是周日"
        >
          <DatePicker.RangePicker style={{ width: '100%' }} disabledDate={semesterDisabledDate} />
        </Form.Item>
        <Space>
          <Button type="primary" loading={saving} onClick={submit}>
            创建
          </Button>
          <Button onClick={onCancel}>取消</Button>
        </Space>
      </Form>
    </Modal>
  )
}
