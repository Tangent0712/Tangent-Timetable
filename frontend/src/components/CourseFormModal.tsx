import { useEffect, useMemo, useState } from 'react'
import { Button, Checkbox, Form, Input, Modal, Popconfirm, Select, Space, Typography } from 'antd'
import type { Course, CoursePayload, PeriodConfig } from '../types'
import { WEEK_DAY_NAMES, parseWeeksInput, rangeWeeks } from '../utils/schedule'

interface Props {
  open: boolean
  course: Course | null
  defaults: { dayOfWeek?: number; startPeriod?: number } | null
  periods: PeriodConfig[]
  totalWeeks: number
  currentWeek: number
  saving: boolean
  onCancel: () => void
  onSubmit: (payload: CoursePayload) => void
  onDelete?: (course: Course) => void
}

interface FormValues {
  name: string
  location?: string
  teacher?: string
  dayOfWeek: number
  startPeriod: number
  endPeriod: number
  weeks: number[]
}

export default function CourseFormModal({
  open,
  course,
  defaults,
  periods,
  totalWeeks,
  currentWeek,
  saving,
  onCancel,
  onSubmit,
  onDelete,
}: Props) {
  const [form] = Form.useForm<FormValues>()
  const [weeksText, setWeeksText] = useState('')

  const periodOptions = useMemo(
    () =>
      [...periods]
        .sort((a, b) => a.periodNumber - b.periodNumber)
        .map((p) => ({
          value: p.periodNumber,
          label: `第${p.periodNumber}节 ${p.startTime?.slice(0, 5)}-${p.endTime?.slice(0, 5)}`,
        })),
    [periods],
  )

  const allWeeks = useMemo(() => rangeWeeks(totalWeeks), [totalWeeks])

  useEffect(() => {
    if (!open) return
    if (course) {
      form.setFieldsValue({
        name: course.name,
        location: course.location ?? undefined,
        teacher: course.teacher ?? undefined,
        dayOfWeek: course.dayOfWeek,
        startPeriod: course.startPeriod,
        endPeriod: course.endPeriod,
        weeks: course.weeks ?? [],
      })
    } else {
      form.resetFields()
      form.setFieldsValue({
        dayOfWeek: defaults?.dayOfWeek ?? 1,
        startPeriod: defaults?.startPeriod ?? 1,
        endPeriod: defaults?.startPeriod ?? 1,
        weeks: allWeeks,
      })
    }
    setWeeksText('')
  }, [open, course, defaults, form, allWeeks])

  const applyWeeksText = () => {
    const parsed = parseWeeksInput(weeksText).filter((w) => w >= 1 && w <= totalWeeks)
    if (parsed.length > 0) form.setFieldValue('weeks', parsed)
  }

  const handleOk = async () => {
    const values = await form.validateFields()
    if (values.endPeriod < values.startPeriod) {
      form.setFields([{ name: 'endPeriod', errors: ['结束节次不能早于开始节次'] }])
      return
    }
    onSubmit({
      name: values.name.trim(),
      location: values.location?.trim() || null,
      teacher: values.teacher?.trim() || null,
      dayOfWeek: values.dayOfWeek,
      startPeriod: values.startPeriod,
      endPeriod: values.endPeriod,
      weeks: [...values.weeks].sort((a, b) => a - b),
    })
  }

  return (
    <Modal
      open={open}
      maskClosable={false}
      title={course ? '编辑课程' : '添加课程'}
      onCancel={onCancel}
      width={620}
      destroyOnHidden
      footer={
        <Space>
          {course && onDelete && (
            <Popconfirm
              title="确定删除这条课程吗？"
              onConfirm={() => onDelete(course)}
              okText="删除"
              cancelText="取消"
            >
              <Button danger>删除</Button>
            </Popconfirm>
          )}
          <Button onClick={onCancel}>取消</Button>
          <Button type="primary" loading={saving} onClick={handleOk}>
            保存
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" requiredMark={false}>
        <Form.Item
          name="name"
          label="课程名称"
          rules={[{ required: true, message: '请输入课程名称' }]}
        >
          <Input placeholder="如 高等数学" />
        </Form.Item>

        <Space size={12} wrap style={{ display: 'flex' }}>
          <Form.Item name="location" label="上课地点" style={{ flex: 1, minWidth: 180 }}>
            <Input placeholder="如 教3-101（可留空）" />
          </Form.Item>
          <Form.Item name="teacher" label="教师" style={{ flex: 1, minWidth: 180 }}>
            <Input placeholder="如 王老师（可留空）" />
          </Form.Item>
        </Space>

        <Space size={12} wrap style={{ display: 'flex' }}>
          <Form.Item
            name="dayOfWeek"
            label="星期"
            rules={[{ required: true }]}
            style={{ flex: 1, minWidth: 120 }}
          >
            <Select options={WEEK_DAY_NAMES.map((n, i) => ({ value: i + 1, label: n }))} />
          </Form.Item>
          <Form.Item
            name="startPeriod"
            label="开始节次"
            rules={[{ required: true }]}
            style={{ flex: 1, minWidth: 120 }}
          >
            <Select options={periodOptions} />
          </Form.Item>
          <Form.Item
            name="endPeriod"
            label="结束节次"
            rules={[{ required: true }]}
            style={{ flex: 1, minWidth: 120 }}
          >
            <Select options={periodOptions} />
          </Form.Item>
        </Space>

        <Form.Item
          name="weeks"
          label="上课周次"
          rules={[{ required: true, message: '请至少选择一个周次' }]}
        >
          <Checkbox.Group
            options={allWeeks.map((w) => ({
              value: w,
              label: w === currentWeek ? `${w}*` : `${w}`,
            }))}
          />
        </Form.Item>

        <Space.Compact style={{ width: '100%' }}>
          <Input
            placeholder="快速输入周次，如 1-16 或 1,3,5-8"
            value={weeksText}
            onChange={(e) => setWeeksText(e.target.value)}
            onPressEnter={applyWeeksText}
          />
          <Button onClick={applyWeeksText}>应用</Button>
          <Button onClick={() => form.setFieldValue('weeks', allWeeks)}>全选</Button>
          <Button
            onClick={() =>
              form.setFieldValue(
                'weeks',
                allWeeks.filter((w) => w % 2 === 1),
              )
            }
          >
            单周
          </Button>
          <Button
            onClick={() =>
              form.setFieldValue(
                'weeks',
                allWeeks.filter((w) => w % 2 === 0),
              )
            }
          >
            双周
          </Button>
          <Button onClick={() => form.setFieldValue('weeks', [currentWeek])}>仅本周</Button>
        </Space.Compact>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          带 * 的周次为当前周
        </Typography.Text>
      </Form>
    </Modal>
  )
}
