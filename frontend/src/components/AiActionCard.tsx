import { Button, Card, Space, Table, Tag, Tooltip, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { AiAction } from '../types'
import { formatWeeks, weekDayName } from '../utils/schedule'

const ACTION_LABELS: Record<string, string> = {
  CREATE_COURSE: '添加课程',
  UPDATE_COURSE: '修改课程',
  DELETE_COURSE: '删除课程',
  CREATE_TODO: '添加待办',
  UPDATE_TODO: '修改待办',
  DELETE_TODO: '删除待办',
  TOGGLE_TODO: '切换待办状态',
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
            {action.scope === 'COURSE' ? '课表' : '待办'}
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

function renderBody(type: string, data: Dict) {
  const before = (data._before as Dict[]) ?? []

  if (type === 'UPDATE_COURSE') {
    const after = (data.courses as Dict[]) ?? []
    return (
      <Space direction="vertical" style={{ width: '100%' }} size={12}>
        {after.map((a, i) => {
          const b = before.find((x) => x.id === a.id) ?? {}
          return (
            <div key={i}>
              <Typography.Text strong>{str(b.name ?? a.name)}</Typography.Text>
              <Table
                size="small"
                rowKey="key"
                columns={DIFF_COLUMNS}
                dataSource={courseDiff(b, a)}
                pagination={false}
                style={{ marginTop: 6 }}
              />
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
            <div style={{ color: '#595959', fontSize: 13 }}>
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
            <div style={{ color: '#595959', fontSize: 13 }}>截止 {str(t.ddl)}</div>
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
            <div style={{ color: '#595959', fontSize: 13 }}>
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
