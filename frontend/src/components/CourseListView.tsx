import { useMemo, useState } from 'react'
import { Button, Card, Input, Popconfirm, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DeleteOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons'
import type { Course, PeriodConfig } from '../types'
import { courseColor } from '../utils/color'
import { WEEK_DAY_NAMES, formatWeeks, weekDayName } from '../utils/schedule'

interface Props {
  courses: Course[]
  periods: PeriodConfig[]
  onEdit: (course: Course) => void
  onDelete: (course: Course) => void
}

/**
 * 全部课程视图。
 *
 * 不渲染到时间网格上：单双周错开的课程在网格里占同一格却并不真正冲突，
 * 网格无法表达这种差异。因此这里直接把数据库记录逐条列出，
 * 完整展示星期、节次、周次、地点、教师，信息无损。
 */
export default function CourseListView({ courses, periods, onEdit, onDelete }: Props) {
  const [keyword, setKeyword] = useState('')

  const periodTimeMap = useMemo(() => {
    const map = new Map<number, string>()
    periods.forEach((p) =>
      map.set(p.periodNumber, `${p.startTime?.slice(0, 5)}-${p.endTime?.slice(0, 5)}`),
    )
    return map
  }, [periods])

  const data = useMemo(() => {
    const kw = keyword.trim().toLowerCase()
    const filtered = kw
      ? courses.filter((c) =>
          [c.name, c.location, c.teacher]
            .filter(Boolean)
            .some((v) => String(v).toLowerCase().includes(kw)),
        )
      : courses
    return [...filtered].sort(
      (a, b) => a.dayOfWeek - b.dayOfWeek || a.startPeriod - b.startPeriod || a.id - b.id,
    )
  }, [courses, keyword])

  const timeRange = (c: Course) => {
    const start = periodTimeMap.get(c.startPeriod)?.split('-')[0]
    const end = periodTimeMap.get(c.endPeriod)?.split('-')[1]
    return start && end ? `${start}-${end}` : '—'
  }

  const columns: ColumnsType<Course> = [
    {
      title: '课程名称',
      dataIndex: 'name',
      width: 200,
      render: (name: string) => {
        const color = courseColor(name)
        return (
          <Space size={6}>
            <span
              style={{
                display: 'inline-block',
                width: 3,
                height: 16,
                background: color.border,
                borderRadius: 2,
              }}
            />
            <Typography.Text strong>{name}</Typography.Text>
          </Space>
        )
      },
    },
    {
      title: '星期',
      dataIndex: 'dayOfWeek',
      width: 80,
      filters: WEEK_DAY_NAMES.map((n, i) => ({ text: n, value: i + 1 })),
      onFilter: (value, record) => record.dayOfWeek === value,
      render: (d: number) => weekDayName(d),
    },
    {
      title: '节次',
      width: 100,
      render: (_, c) => `第 ${c.startPeriod}-${c.endPeriod} 节`,
    },
    {
      title: '时间',
      width: 120,
      render: (_, c) => (
        <Typography.Text type="secondary">{timeRange(c)}</Typography.Text>
      ),
    },
    {
      title: '周次',
      dataIndex: 'weeks',
      render: (weeks: number[]) => {
        if (!weeks || weeks.length === 0) return <span className="text-muted">—</span>
        const odd = weeks.every((w) => w % 2 === 1)
        const even = weeks.every((w) => w % 2 === 0)
        return (
          <Space size={4} wrap>
            <span>{formatWeeks(weeks)}</span>
            {weeks.length > 1 && odd && <Tag color="orange">单周</Tag>}
            {weeks.length > 1 && even && <Tag color="cyan">双周</Tag>}
            <Typography.Text type="secondary">共 {weeks.length} 周</Typography.Text>
          </Space>
        )
      },
    },
    {
      title: '地点',
      dataIndex: 'location',
      width: 150,
      render: (v: string | null) => v || <span className="text-muted">未指定</span>,
    },
    {
      title: '教师',
      dataIndex: 'teacher',
      width: 110,
      render: (v: string | null) => v || <span className="text-muted">未指定</span>,
    },
    {
      title: '操作',
      width: 90,
      fixed: 'right',
      render: (_, course) => (
        <Space size={0}>
          <Button type="text" icon={<EditOutlined />} onClick={() => onEdit(course)} />
          <Popconfirm
            title="确定删除这条课程吗？"
            okText="删除"
            cancelText="取消"
            onConfirm={() => onDelete(course)}
          >
            <Button type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card size="small">
      <Space style={{ marginBottom: 12, justifyContent: 'space-between', width: '100%' }}>
        <Input
          prefix={<SearchOutlined />}
          placeholder="搜索课程名 / 地点 / 教师"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          allowClear
          style={{ width: 260 }}
        />
        <Typography.Text type="secondary">
          共 {data.length} 条上课记录
          {keyword && ` / 全部 ${courses.length} 条`}
        </Typography.Text>
      </Space>

      <Table
        size="small"
        rowKey="id"
        columns={columns}
        dataSource={data}
        pagination={false}
        scroll={{ x: 900 }}
        locale={{ emptyText: keyword ? '没有匹配的课程' : '该课表还没有课程' }}
      />
    </Card>
  )
}
