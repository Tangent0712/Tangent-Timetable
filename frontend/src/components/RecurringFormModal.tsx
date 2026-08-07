import { useEffect, useMemo, useState } from 'react'
import {
  Alert,
  Form,
  InputNumber,
  Input,
  Modal,
  Radio,
  Select,
  Space,
  Switch,
  TimePicker,
  Typography,
} from 'antd'
import type { Dayjs } from 'dayjs'
import type { RecurringFrequency, RecurringTodo, RecurringTodoPayload } from '../types'
import { WEEK_DAY_NAMES, dayjs } from '../utils/schedule'
import { describeOffset } from '../utils/recurring'

interface Props {
  open: boolean
  rule: RecurringTodo | null
  saving: boolean
  onCancel: () => void
  onSubmit: (payload: RecurringTodoPayload) => void
}

interface FormValues {
  title: string
  frequency: RecurringFrequency
  dayOfWeek?: number
  dayOfMonth?: number
  triggerTime: Dayjs
  offsetDays: number
  offsetHours: number
  offsetMinutes: number
  chainAfterComplete: boolean
}

const TIME_FORMAT = 'HH:mm'

export default function RecurringFormModal({
  open,
  rule,
  saving,
  onCancel,
  onSubmit,
}: Props) {
  const [form] = Form.useForm<FormValues>()
  const [frequency, setFrequency] = useState<RecurringFrequency>('WEEKLY')
  const [preview, setPreview] = useState<{ trigger?: Dayjs; total: number }>({ total: 0 })

  useEffect(() => {
    if (!open) return
    if (rule) {
      const m = rule.ddlOffsetMinutes
      form.setFieldsValue({
        title: rule.title,
        frequency: rule.frequency,
        dayOfWeek: rule.dayOfWeek ?? undefined,
        dayOfMonth: rule.dayOfMonth ?? undefined,
        triggerTime: dayjs(rule.triggerTime, 'HH:mm:ss'),
        offsetDays: Math.floor(m / 1440),
        offsetHours: Math.floor((m % 1440) / 60),
        offsetMinutes: m % 60,
        chainAfterComplete: rule.chainAfterComplete,
      })
      setFrequency(rule.frequency)
    } else {
      form.resetFields()
      form.setFieldsValue({
        frequency: 'WEEKLY',
        dayOfWeek: 5,
        triggerTime: dayjs('12:00', TIME_FORMAT),
        offsetDays: 2,
        offsetHours: 0,
        offsetMinutes: 0,
        chainAfterComplete: false,
      })
      setFrequency('WEEKLY')
    }
  }, [open, rule, form])

  // 实时预览：触发时刻 → 截止时刻
  const refreshPreview = () => {
    const v = form.getFieldsValue()
    const total =
      (v.offsetDays ?? 0) * 1440 + (v.offsetHours ?? 0) * 60 + (v.offsetMinutes ?? 0)
    setPreview({ trigger: v.triggerTime, total })
  }

  useEffect(() => {
    if (open) refreshPreview()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  const previewText = useMemo(() => {
    if (!preview.trigger || preview.total <= 0) return null
    const t = preview.trigger
    const end = t.add(preview.total, 'minute')
    const dayDiff = end.startOf('day').diff(t.startOf('day'), 'day')
    const suffix = dayDiff === 0 ? '当天' : `${dayDiff} 天后`
    return `触发于 ${t.format(TIME_FORMAT)}，截止于 ${suffix} ${end.format(TIME_FORMAT)}（共 ${describeOffset(preview.total)}）`
  }, [preview])

  const handleOk = async () => {
    const v = await form.validateFields()
    const total =
      (v.offsetDays ?? 0) * 1440 + (v.offsetHours ?? 0) * 60 + (v.offsetMinutes ?? 0)
    if (total <= 0) {
      form.setFields([{ name: 'offsetDays', errors: ['截止时间必须晚于触发时间'] }])
      return
    }
    onSubmit({
      title: v.title.trim(),
      frequency: v.frequency,
      dayOfWeek: v.frequency === 'WEEKLY' ? (v.dayOfWeek ?? null) : null,
      dayOfMonth: v.frequency === 'MONTHLY' ? (v.dayOfMonth ?? null) : null,
      triggerTime: v.triggerTime.format('HH:mm:ss'),
      ddlOffsetMinutes: total,
      enabled: rule ? rule.enabled : true,
      chainAfterComplete: v.chainAfterComplete,
    })
  }

  return (
    <Modal
      open={open}
      title={rule ? '编辑循环任务' : '新建循环任务'}
      onCancel={onCancel}
      onOk={handleOk}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        onValuesChange={refreshPreview}
      >
        <Form.Item
          name="title"
          label="待办标题"
          rules={[{ required: true, message: '请输入标题' }]}
        >
          <Input placeholder="如 刷本周网课" />
        </Form.Item>

        <Form.Item name="frequency" label="重复频率" rules={[{ required: true }]}>
          <Radio.Group
            onChange={(e) => setFrequency(e.target.value)}
            options={[
              { value: 'DAILY', label: '每天' },
              { value: 'WEEKLY', label: '每周' },
              { value: 'MONTHLY', label: '每月' },
            ]}
            optionType="button"
          />
        </Form.Item>

        {frequency === 'WEEKLY' && (
          <Form.Item
            name="dayOfWeek"
            label="星期"
            rules={[{ required: true, message: '请选择星期' }]}
          >
            <Select
              options={WEEK_DAY_NAMES.map((n, i) => ({ value: i + 1, label: n }))}
              style={{ width: 140 }}
            />
          </Form.Item>
        )}

        {frequency === 'MONTHLY' && (
          <Form.Item
            name="dayOfMonth"
            label="每月几号"
            rules={[{ required: true, message: '请选择日期' }]}
            extra="若当月没有该日期（如 31 号遇到 2 月），自动顺延到当月最后一天"
          >
            <InputNumber min={1} max={31} style={{ width: 140 }} addonAfter="号" />
          </Form.Item>
        )}

        <Form.Item
          name="triggerTime"
          label="触发时刻"
          rules={[{ required: true, message: '请选择触发时刻' }]}
        >
          <TimePicker format={TIME_FORMAT} minuteStep={5} allowClear={false} />
        </Form.Item>

        <Form.Item label="截止时间（从触发时刻往后推算）" required>
          <Space>
            <Form.Item name="offsetDays" noStyle>
              <InputNumber min={0} max={60} addonAfter="天" style={{ width: 100 }} />
            </Form.Item>
            <Form.Item name="offsetHours" noStyle>
              <InputNumber min={0} max={23} addonAfter="时" style={{ width: 100 }} />
            </Form.Item>
            <Form.Item name="offsetMinutes" noStyle>
              <InputNumber min={0} max={59} addonAfter="分" style={{ width: 100 }} />
            </Form.Item>
          </Space>
        </Form.Item>

        {previewText && (
          <Alert type="info" showIcon message={previewText} style={{ marginBottom: 16 }} />
        )}

        <Form.Item
          name="chainAfterComplete"
          label="完成后立即生成下一期"
          valuePropName="checked"
          extra={
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              关闭时只按触发时刻生成（推荐）；开启后你一完成当期，下一期立刻出现，
              适合「做完就接着做下一个」的任务。
            </Typography.Text>
          }
        >
          <Switch />
        </Form.Item>
      </Form>
    </Modal>
  )
}
