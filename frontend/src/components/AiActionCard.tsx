import { Button, Card, Space, Table, Tag, Tooltip, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { AiAction } from '../types'
import { formatWeeks, weekDayName } from '../utils/schedule'
import { describeOffset } from '../utils/recurring'

const ACTION_LABELS: Record<string, string> = {
  CREATE_COURSE: '添加课程',
  UPDATE_COURSE: '修改课程',
  DELETE_COURSE: '删除课程',
  CREATE_TODO: '添加待办',
  UPDATE_TODO: '修改待办',
  DELETE_TODO: '删除待办',
  TOGGLE_TODO: '切换待办状态',
  CREATE_RECURRING: '添加循环任务',
  UPDATE_RECURRING: '修改循环任务',
  DELETE_RECURRING: '删除循环任务',
  TOGGLE_RECURRING: '启用/停用循环任务',
}

interface Props {
  action: AiAction
  executing: boolean
  onExecute: () => void
  onReject: () => void
}

type Dict = Record<string, unknown>

interface DiffRow {
  key: string
  field: string
  before: string
  after: string
  changed: boolean
}

export default function AiActionCard({ action, executing, onExecute, onReject }: Props) {
  const data = (action.data ?? {}) as Dict
  const pending = action.status === 'PENDING'
  const stale = action.status === 'STALE'

  const statusTag =
    action.status === 'EXECUTED' ? (
      <Tag color="green">已执行</Tag>
    ) : action.status === 'REJECTED' ? (
      <Tag>已取消</Tag>
    ) : stale ? (
      <Tooltip title="相关数据已变化，或已有更新的提案，此提案不能再执行">
        <Tag color="default">已失效</Tag>
      </Tooltip>
    ) : (
      <Tag color="orange">待确认</Tag>
    )

  return (
    <Card
      className="chat-action-card"
      size="small"
      style={stale ? { opacity: 0.6 } : undefined}
      title={
        <Space>
          <span>{ACTION_LABELS[action.type] ?? action.type}</span>
          <Tag color={action.scope === 'COURSE' ? 'geekblue' : 'purple'}>
            {action.scope === 'COURSE'
              ? '课表'
              : action.type.includes('RECURRING')
                ? '循环任务'
                : '待办'}
          </Tag>
          {statusTag}
        </Space>
      }
      extra={
        pending && (
          <Space>
            <Button size="small" type="primary" loading={executing} onClick={onExecute}>
              确认执行
            </Button>
            <Button size="small" onClick={onReject}>
              取消
            </Button>
          </Space>
        )
      }
    >
      {renderBody(action.type, data)}
      {stale && (
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          数据已变动，请重新向 AI 提问以获得最新提案。
        </Typography.Text>
      )}
    </Card>
  )
}

const DIFF_COLUMNS: ColumnsType<DiffRow> = [
  { title: '字段', dataIndex: 'field', width: 90 },
  {
    title: '修改前',
    dataIndex: 'before',
    render: (v: string, row) => (
      <Typography.Text delete={row.changed} type={row.changed ? 'secondary' : undefined}>
        {v}
      </Typography.Text>
    ),
  },
  {
    title: '修改后',
    dataIndex: 'after',
    render: (v: string, row) => (
      <Typography.Text strong={row.changed} type={row.changed ? 'danger' : undefined}>
        {v}
      </Typography.Text>
    ),
  },
]

function recurringLine(r: Dict): string {
  const offset = describeOffset(Number(r.ddlOffsetMinutes))
  const chain = r.chainAfterComplete ? '，完成后接下一期' : ''
  if (r.frequency === 'CUSTOM') {
    return `自定义脚本判断触发，触发后 ${offset} 截止${chain}`
  }
  const time = String(r.triggerTime ?? '').slice(0, 5) || '--:--'
  let when: string
  switch (r.frequency) {
    case 'DAILY':
      when = `每天 ${time}`
      break
    case 'WEEKLY':
      when = `每${weekDayName(Number(r.dayOfWeek)).replace('周', '周')} ${time}`
      break
    case 'MONTHLY':
      when = `每月 ${r.dayOfMonth} 号 ${time}`
      break
    default:
      when = time
  }
  return `${when} 触发，触发后 ${offset} 截止${chain}`
}

function recurringDiff(b: Dict, a: Dict): DiffRow[] {
  const rows: [string, string, string][] = [
    ['标题', str(b.title), str(a.title)],
    ['触发规则', b.frequency ? recurringLine(b) : '未指定', a.frequency ? recurringLine(a) : '未指定'],
    ['脚本', scriptSummary(b.script), scriptSummary(a.script)],
  ]
  return rows.map(([field, before, after]) => ({
    key: field,
    field,
    before,
    after,
    changed: before !== after,
  }))
}

function scriptSummary(v: unknown): string {
  if (v === null || v === undefined || v === '') return '无'
  const s = String(v)
  return s.length > 40 ? s.slice(0, 40) + '…' : s
}

function renderBody(type: string, data: Dict) {
  const before = (data._before as Dict[]) ?? []

  if (type === 'CREATE_RECURRING') {
    const rules = (data.rules as Dict[]) ?? []
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        {rules.map((r, i) => (
          <div key={i}>
            <Typography.Text strong>{str(r.title)}</Typography.Text>
            <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{recurringLine(r)}</div>
            {r.script ? (
              <pre className="ai-script-block">{String(r.script)}</pre>
            ) : null}
          </div>
        ))}
      </Space>
    )
  }

  if (type === 'UPDATE_RECURRING') {
    const after = (data.rules as Dict[]) ?? []
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        {after.map((a, i) => {
          const b = before.find((x) => x.id === a.id) ?? {}
          const scriptChanged = Boolean(a.script) && String(a.script) !== String(b.script ?? '')
          return (
            <div key={i}>
              <Table
                size="small"
                rowKey="key"
                columns={DIFF_COLUMNS}
                dataSource={recurringDiff(b, a)}
                pagination={false}
              />
              {scriptChanged ? (
                <pre className="ai-script-block">{String(a.script)}</pre>
              ) : null}
            </div>
          )
        })}
      </Space>
    )
  }

  if (type === 'DELETE_RECURRING' || type === 'TOGGLE_RECURRING') {
    if (before.length > 0) {
      return (
        <Space direction="vertical" style={{ width: '100%' }} size={8}>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {type === 'TOGGLE_RECURRING'
              ? '将切换以下循环任务的启用状态：'
              : '将删除以下循环任务（已生成的待办保留）：'}
          </Typography.Text>
          {before.map((r, i) => (
            <div key={i}>
              <Typography.Text delete={type === 'DELETE_RECURRING'} strong>
                {str(r.title)}
              </Typography.Text>
              <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
                {recurringLine(r)}
                {type === 'TOGGLE_RECURRING' &&
                  `（当前${r.enabled ? '启用' : '停用'} → ${r.enabled ? '停用' : '启用'}）`}
              </div>
            </div>
          ))}
        </Space>
      )
    }
    const ids = data.recurringIds as unknown[] | undefined
    if (Array.isArray(ids)) return <span>目标 ID：{ids.join(', ')}</span>
  }

  if (type === 'UPDATE_COURSE') {
    const after = (data.courses as Dict[]) ?? []
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        {after.map((a, i) => {
          const b = before.find((x) => x.id === a.id) ?? {}
          // 只展示发生变化的字段；其余字段按课程摘要一行展示
          const changedRows = courseDiff(b, a).filter((r) => r.changed)
          return (
            <div key={i}>
              <Typography.Text strong>{str(a.name ?? b.name)}</Typography.Text>
              <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{courseLine(a)}</div>
              {changedRows.length > 0 && (
                <Table
                  size="small"
                  rowKey="key"
                  columns={DIFF_COLUMNS}
                  dataSource={changedRows}
                  pagination={false}
                  style={{ marginTop: 6 }}
                />
              )}
            </div>
          )
        })}
      </Space>
    )
  }

  if (type === 'UPDATE_TODO') {
    const after = (data.todos as Dict[]) ?? []
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        {after.map((a, i) => {
          const b = before.find((x) => x.id === a.id) ?? {}
          return (
            <Table
              key={i}
              size="small"
              rowKey="key"
              columns={DIFF_COLUMNS}
              dataSource={todoDiff(b, a)}
              pagination={false}
            />
          )
        })}
      </Space>
    )
  }

  if (type === 'CREATE_COURSE') {
    const courses = (data.courses as Dict[]) ?? []
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        {courses.map((c, i) => (
          <div key={i}>
            <Typography.Text strong>{str(c.name)}</Typography.Text>
            <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
              {courseLine(c)}
            </div>
          </div>
        ))}
      </Space>
    )
  }

  if (type === 'CREATE_TODO') {
    const todos = (data.todos as Dict[]) ?? []
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        {todos.map((t, i) => (
          <div key={i}>
            <Typography.Text strong>{str(t.title)}</Typography.Text>
            <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>截止 {str(t.ddl)}</div>
          </div>
        ))}
      </Space>
    )
  }

  // DELETE_COURSE / DELETE_TODO / TOGGLE_TODO：展示将被影响的原始记录
  if (before.length > 0) {
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={8}>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {type === 'TOGGLE_TODO' ? '将切换以下待办的完成状态：' : '将删除以下记录：'}
        </Typography.Text>
        {before.map((b, i) => (
          <div key={i}>
            <Typography.Text delete={type !== 'TOGGLE_TODO'} strong>
              {str(b.name ?? b.title)}
            </Typography.Text>
            <div style={{ color: 'var(--text-secondary)', fontSize: 13 }}>
              {b.dayOfWeek ? courseLine(b) : `截止 ${str(b.ddl)}`}
            </div>
          </div>
        ))}
      </Space>
    )
  }

  const ids = (data.courseIds ?? data.todoIds) as unknown[] | undefined
  if (Array.isArray(ids)) {
    return <span>目标 ID：{ids.join(', ')}</span>
  }

  return <pre style={{ margin: 0 }}>{JSON.stringify(data, null, 2)}</pre>
}

function courseDiff(b: Dict, a: Dict): DiffRow[] {
  const rows: [string, string, string][] = [
    ['课程名', str(b.name), str(a.name)],
    ['星期', dayStr(b.dayOfWeek), dayStr(a.dayOfWeek)],
    ['节次', periodStr(b), periodStr(a)],
    ['周次', weeksStr(b.weeks), weeksStr(a.weeks)],
    ['地点', str(b.location), str(a.location)],
    ['教师', str(b.teacher), str(a.teacher)],
  ]
  return rows.map(([field, before, after]) => ({
    key: field,
    field,
    before,
    after,
    changed: before !== after,
  }))
}

function todoDiff(b: Dict, a: Dict): DiffRow[] {
  const rows: [string, string, string][] = [
    ['标题', str(b.title), str(a.title)],
    ['截止时间', str(b.ddl), str(a.ddl)],
  ]
  return rows.map(([field, before, after]) => ({
    key: field,
    field,
    before,
    after,
    changed: before !== after,
  }))
}

function courseLine(c: Dict): string {
  const parts = [dayStr(c.dayOfWeek), periodStr(c), `周次 ${weeksStr(c.weeks)}`]
  if (c.location) parts.push(String(c.location))
  if (c.teacher) parts.push(String(c.teacher))
  return parts.join(' · ')
}

function dayStr(v: unknown): string {
  return typeof v === 'number' ? weekDayName(v) : '未指定'
}

function periodStr(c: Dict): string {
  return c.startPeriod && c.endPeriod ? `第 ${c.startPeriod}-${c.endPeriod} 节` : '未指定'
}

function weeksStr(v: unknown): string {
  return Array.isArray(v) && v.length > 0 ? formatWeeks(v as number[]) : '未指定'
}

function str(value: unknown): string {
  if (value === null || value === undefined || value === '') return '未指定'
  return String(value)
}
