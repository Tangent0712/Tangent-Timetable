import { useEffect, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Input,
  Select,
  Slider,
  Space,
  Table,
  TimePicker,
  Typography,
  message,
} from 'antd'
import { CopyOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { Dayjs } from 'dayjs'
import { periodApi } from '../api'
import { getApiKey } from '../api/client'
import { useApp } from '../store/AppContext'
import { useFont } from '../store/FontContext'
import type { PeriodCategory, PeriodConfig } from '../types'
import { dayjs } from '../utils/schedule'

const TIME_FORMAT = 'HH:mm'

/** 小组件展示页基于当前站点地址生成，随部署环境自动适配 */
const WIDGET_API_BASE = window.location.origin

/** 可编辑全局作息时间表的账号 label（前端仅做 UI 限制，后端同样校验） */
const ADMIN_LABELS = (import.meta.env.VITE_ADMIN_LABELS ?? 'Tangent0712')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean)

const CATEGORY_OPTIONS: { value: PeriodCategory; label: string }[] = [
  { value: 'MORNING', label: '上午' },
  { value: 'AFTERNOON', label: '下午' },
  { value: 'EVENING', label: '晚上' },
]

export default function SettingsPage() {
  const {
    periods,
    refreshPeriods,
    label,
    schedules,
    activeScheduleId,
    activeSchedule,
    setActiveScheduleId,
  } = useApp()
  const { globalScale, ttScale, setGlobalScale, setTtScale } = useFont()
  const [rows, setRows] = useState<PeriodConfig[]>([])
  const [widgetKey] = useState(() => getApiKey() ?? '')

  // 桌面小组件只读展示页地址（含 key 与当前课表）
  const widgetPageUrl = `${WIDGET_API_BASE}/widget?key=${encodeURIComponent(widgetKey)}${
    activeScheduleId ? `&scheduleId=${activeScheduleId}` : ''
  }`

  const copyWidgetApi = async () => {
    if (!activeScheduleId) {
      message.warning('请先选择一张课表')
      return
    }
    try {
      await navigator.clipboard.writeText(widgetPageUrl)
      message.success('已复制小组件网页地址')
    } catch {
      message.error('复制失败，请手动选择复制')
    }
  }

  // 作息时间表仅管理员账号可编辑，其余用户只读
  const canEdit = ADMIN_LABELS.includes(label)
  const [saving, setSaving] = useState(false)
  const [dirty, setDirty] = useState(false)

  useEffect(() => {
    setRows(
      periods.map((p) => ({
        ...p,
        startTime: p.startTime.slice(0, 5),
        endTime: p.endTime.slice(0, 5),
      })),
    )
    setDirty(false)
  }, [periods])

  const patch = (periodNumber: number, changes: Partial<PeriodConfig>) => {
    setRows((prev) => prev.map((r) => (r.periodNumber === periodNumber ? { ...r, ...changes } : r)))
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
    { title: '节次', dataIndex: 'periodNumber', width: 44, render: (v: number) => `${v}` },
    {
      title: '开始时间',
      dataIndex: 'startTime',
      width: 110,
      render: (v: string, row) => (
        <TimePicker
          value={dayjs(v, TIME_FORMAT)}
          format={TIME_FORMAT}
          allowClear={false}
          minuteStep={5}
          open={canEdit ? undefined : false}
          inputReadOnly={!canEdit}
          style={{ width: '100%' }}
          onChange={(t: Dayjs | null) =>
            t && patch(row.periodNumber, { startTime: t.format(TIME_FORMAT) })
          }
        />
      ),
    },
    {
      title: '结束时间',
      dataIndex: 'endTime',
      width: 110,
      render: (v: string, row) => (
        <TimePicker
          value={dayjs(v, TIME_FORMAT)}
          format={TIME_FORMAT}
          allowClear={false}
          minuteStep={5}
          open={canEdit ? undefined : false}
          inputReadOnly={!canEdit}
          style={{ width: '100%' }}
          onChange={(t: Dayjs | null) =>
            t && patch(row.periodNumber, { endTime: t.format(TIME_FORMAT) })
          }
        />
      ),
    },
    {
      title: '时段',
      dataIndex: 'category',
      width: 85,
      render: (v: PeriodCategory, row) => (
        <Select
          value={v}
          options={CATEGORY_OPTIONS}
          style={{ width: '100%' }}
          size="small"
          open={canEdit ? undefined : false}
          suffixIcon={canEdit ? undefined : null}
          onChange={(c) => patch(row.periodNumber, { category: c })}
        />
      ),
    },
    {
      title: '时长',
      width: 70,
      responsive: ['md', 'lg', 'xl', 'xxl'],
      render: (_, row) => {
        const minutes = dayjs(row.endTime, TIME_FORMAT).diff(
          dayjs(row.startTime, TIME_FORMAT),
          'minute',
        )
        return minutes > 0 ? (
          `${minutes} 分钟`
        ) : (
          <Typography.Text type="danger">无效</Typography.Text>
        )
      },
    },
  ]

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card
        title="桌面小组件网页地址"
        extra={
          <Button type="primary" icon={<CopyOutlined />} onClick={copyWidgetApi}>
            一键复制
          </Button>
        }
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="复制下面的网页地址，填入支持加载网页（WebView）的桌面小组件 App（iPad / Android），即可在小尺寸小组件中直接展示本课表的课程与待办。若使用全屏应用，直接打开本站首页即可（已适配窄屏）。"
          />
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <Typography.Text strong>选择课表</Typography.Text>
            <Select
              style={{ minWidth: 220, flex: 1 }}
              placeholder="选择课表"
              value={activeScheduleId ?? undefined}
              onChange={(id) => setActiveScheduleId(id)}
              options={schedules.map((s) => ({ label: s.name, value: s.id }))}
            />
            {activeSchedule && (
              <Typography.Text type="secondary">当前：{activeSchedule.name}</Typography.Text>
            )}
          </div>
          <Input readOnly value={widgetPageUrl} onFocus={(e) => e.currentTarget.select()} />
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            该地址包含你的 API Key，仅供自己使用，请勿公开。
          </Typography.Text>
        </Space>
      </Card>

      <Card
        title="字体大小"
        extra={
          <Button
            size="small"
            onClick={() => {
              setGlobalScale(1)
              setTtScale(1)
            }}
          >
            重置
          </Button>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <Typography.Text strong>全局其它字体</Typography.Text>
              <Typography.Text type="secondary">{globalScale.toFixed(2)}×</Typography.Text>
            </div>
            <Slider
              min={0.8}
              max={1.4}
              step={0.05}
              value={globalScale}
              onChange={setGlobalScale}
              marks={{ 0.8: '小', 1: '默认', 1.4: '大' }}
            />
          </div>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <Typography.Text strong>课程表字体</Typography.Text>
              <Typography.Text type="secondary">{ttScale.toFixed(2)}×</Typography.Text>
            </div>
            <Slider
              min={0.8}
              max={1.4}
              step={0.05}
              value={ttScale}
              onChange={setTtScale}
              marks={{ 0.8: '小', 1: '默认', 1.4: '大' }}
            />
          </div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            设置保存在当前设备（localStorage），不随账号同步。
          </Typography.Text>
        </div>
      </Card>

      <Card
        title="作息时间表"
        extra={
          <Space>
            {dirty && <Typography.Text type="warning">有未保存的修改</Typography.Text>}
            {canEdit && (
              <Button type="primary" loading={saving} disabled={!dirty} onClick={save}>
                保存
              </Button>
            )}
          </Space>
        }
      >
        {canEdit ? (
          <Alert
            type="info"
            showIcon
            message="作息时间表为全局配置，修改后课表视图与 AI 解析都会按新时间计算。"
            style={{ marginBottom: 16 }}
          />
        ) : (
          <Alert
            type="error"
            showIcon
            message="这是南京邮电大学标准作息时间表，仅供阅读，不可编辑！"
            style={{ marginBottom: 16 }}
          />
        )}
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
