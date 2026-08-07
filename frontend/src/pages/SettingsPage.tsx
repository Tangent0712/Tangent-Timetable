import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Popconfirm,
  Select,
  Space,
  Table,
  TimePicker,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import { periodApi } from '../api'
import { useApp } from '../store/AppContext'
import type { PeriodCategory, PeriodConfig } from '../types'
import { dayjs } from '../utils/schedule'

const TIME_FORMAT = 'HH:mm'

const CATEGORY_OPTIONS: { value: PeriodCategory; label: string }[] = [
  { value: 'MORNING', label: '上午' },
  { value: 'AFTERNOON', label: '下午' },
  { value: 'EVENING', label: '晚上' },
]

export default function SettingsPage() {
  const { periods, refreshPeriods, label, logout } = useApp()
  const [rows, setRows] = useState<PeriodConfig[]>([])
  const [saving, setSaving] = useState(false)
  const [dirty, setDirty] = useState(false)

  useEffect(() => {
    setRows(periods.map((p) => ({ ...p, startTime: p.startTime.slice(0, 5), endTime: p.endTime.slice(0, 5) })))
    setDirty(false)
  }, [periods])

  const patch = (periodNumber: number, changes: Partial<PeriodConfig>) => {
    setRows((prev) =>
      prev.map((r) => (r.periodNumber === periodNumber ? { ...r, ...changes } : r)),
    )
    setDirty(true)
  }

  const save = async () => {
    const invalid = rows.find((r) => r.startTime >= r.endTime)
    if (invalid) {
      message.error(`第 ${invalid.periodNumber} 节的结束时间必须晚于开始时间`)
      return
    }
    setSaving(true)
    try {
      await periodApi.update(
        rows.map((r) => ({
          periodNumber: r.periodNumber,
          startTime: `${r.startTime}:00`,
          endTime: `${r.endTime}:00`,
          category: r.category,
        })),
      )
      message.success('作息时间表已保存')
      await refreshPeriods()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const columns: ColumnsType<PeriodConfig> = [
    { title: '节次', dataIndex: 'periodNumber', width: 80, render: (v: number) => `第 ${v} 节` },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      width: 160,
      render: (v: string, row) => (
        <TimePicker
          value={dayjs(v, TIME_FORMAT)}
          format={TIME_FORMAT}
          allowClear={false}
          minuteStep={5}
          onChange={(t: Dayjs | null) =>
            t && patch(row.periodNumber, { startTime: t.format(TIME_FORMAT) })
          }
        />
      ),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      width: 160,
      render: (v: string, row) => (
        <TimePicker
          value={dayjs(v, TIME_FORMAT)}
          format={TIME_FORMAT}
          allowClear={false}
          minuteStep={5}
          onChange={(t: Dayjs | null) =>
            t && patch(row.periodNumber, { endTime: t.format(TIME_FORMAT) })
          }
        />
      ),
    },
    {
      title: '时段',
      dataIndex: 'category',
      width: 140,
      render: (v: PeriodCategory, row) => (
        <Select
          value={v}
          options={CATEGORY_OPTIONS}
          style={{ width: 110 }}
          onChange={(c) => patch(row.periodNumber, { category: c })}
        />
      ),
    },
    {
      title: '时长',
      render: (_, row) => {
        const minutes = dayjs(row.endTime, TIME_FORMAT).diff(
          dayjs(row.startTime, TIME_FORMAT),
          'minute',
        )
        return minutes > 0 ? `${minutes} 分钟` : <Typography.Text type="danger">无效</Typography.Text>
      },
    },
  ]

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card title="账号">
        <Descriptions column={1} size="small">
          <Descriptions.Item label="当前用户">{label || '—'}</Descriptions.Item>
          <Descriptions.Item label="数据同步">每 30 秒自动拉取一次</Descriptions.Item>
        </Descriptions>
        <Popconfirm title="确定登出？本地 API Key 将被清除" onConfirm={logout} okText="登出" cancelText="取消">
          <Button danger>登出</Button>
        </Popconfirm>
      </Card>

      <Card
        title="作息时间表"
        extra={
          <Space>
            {dirty && <Typography.Text type="warning">有未保存的修改</Typography.Text>}
            <Button type="primary" loading={saving} disabled={!dirty} onClick={save}>
              保存
            </Button>
          </Space>
        }
      >
        <Alert
          type="info"
          showIcon
          message="作息时间表为全局配置，修改后课表视图与 AI 解析都会按新时间计算。"
          style={{ marginBottom: 16 }}
        />
        <Table
          size="small"
          rowKey="periodNumber"
          columns={columns}
          dataSource={rows}
          pagination={false}
        />
      </Card>
    </Space>
  )
}
