import { useEffect, useState } from 'react'
import {
  Button,
  DatePicker,
  Form,
  Input,
  List,
  message,
  Modal,
  Popconfirm,
  Space,
  Typography,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import type { Schedule } from '../types'
import { dayjs, totalWeeks } from '../utils/schedule'

interface Props {
  open: boolean
  schedules: Schedule[]
  onCancel: () => void
  onCreate: (payload: Omit<Schedule, 'id'>) => Promise<void>
  onUpdate: (id: number, payload: Omit<Schedule, 'id'>) => Promise<void>
  onDelete: (id: number) => Promise<void>
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

export default function ScheduleManagerModal({
  open,
  schedules,
  onCancel,
  onCreate,
  onUpdate,
  onDelete,
}: Props) {
  const [form] = Form.useForm<FormValues>()
  const [editing, setEditing] = useState<Schedule | null>(null)
  const [saving, setSaving] = useState(false)
  const [formVisible, setFormVisible] = useState(false)

  useEffect(() => {
    if (!open) {
      setEditing(null)
      setFormVisible(false)
      form.resetFields()
    }
  }, [open, form])

  const startEdit = (schedule: Schedule) => {
    setEditing(schedule)
    setFormVisible(true)
    form.setFieldsValue({
      name: schedule.name,
      range: [dayjs(schedule.periodStartDate), dayjs(schedule.periodEndDate)],
    })
  }

  const startCreate = () => {
    setEditing(null)
    setFormVisible(true)
    form.resetFields()
  }

  const submit = async () => {
    const values = await form.validateFields()
    const payload = {
      name: values.name.trim(),
      periodStartDate: values.range[0].format('YYYY-MM-DD'),
      periodEndDate: values.range[1].format('YYYY-MM-DD'),
    }
    setSaving(true)
    try {
      if (editing) await onUpdate(editing.id, payload)
      else await onCreate(payload)
      setFormVisible(false)
      setEditing(null)
      form.resetFields()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await onDelete(id)
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  return (
    <Modal
      open={open}
      maskClosable={false}
      title="课表管理"
      onCancel={onCancel}
      footer={null}
      width={620}
    >
      <List
        dataSource={schedules}
        locale={{ emptyText: '还没有课表，先新建一个' }}
        renderItem={(item) => (
          <List.Item
            actions={[
              <Button
                key="edit"
                type="text"
                icon={<EditOutlined />}
                onClick={() => startEdit(item)}
              />,
              <Popconfirm
                key="del"
                title="删除课表会同时删除其下所有课程，确定吗？"
                okText="删除"
                cancelText="取消"
                onConfirm={() => handleDelete(item.id)}
              >
                <Button type="text" danger icon={<DeleteOutlined />} />
              </Popconfirm>,
            ]}
          >
            <List.Item.Meta
              title={item.name}
              description={`${item.periodStartDate} ~ ${item.periodEndDate} · 共 ${totalWeeks(
                item.periodStartDate,
                item.periodEndDate,
              )} 周`}
            />
          </List.Item>
        )}
      />

      {formVisible ? (
        <Form form={form} layout="vertical" requiredMark={false} style={{ marginTop: 12 }}>
          <Typography.Text strong>{editing ? '编辑课表' : '新建课表'}</Typography.Text>
          <Form.Item
            name="name"
            label="课表名称"
            rules={[{ required: true, message: '请输入课表名称' }]}
            style={{ marginTop: 8 }}
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
              保存
            </Button>
            <Button
              onClick={() => {
                setFormVisible(false)
                setEditing(null)
              }}
            >
              取消
            </Button>
          </Space>
        </Form>
      ) : (
        <Button
          type="dashed"
          block
          icon={<PlusOutlined />}
          onClick={startCreate}
          style={{ marginTop: 12 }}
        >
          新建课表
        </Button>
      )}
    </Modal>
  )
}
