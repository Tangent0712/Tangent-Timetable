import { useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Checkbox,
  DatePicker,
  Empty,
  Form,
  Input,
  List,
  Modal,
  Popconfirm,
  Segmented,
  Space,
  Statistic,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  RetweetOutlined,
  ScheduleOutlined,
} from '@ant-design/icons'
import RecurringTodoPanel from '../components/RecurringTodoPanel'
import type { Dayjs } from 'dayjs'
import { todoApi } from '../api'
import { useApp } from '../store/AppContext'
import type { Todo } from '../types'
import { dayjs, formatCountdown } from '../utils/schedule'

type Filter = 'active' | 'completed' | 'all'

export default function TodosPage() {
  const { todos, recurringTodos, refreshTodos } = useApp()
  const [filter, setFilter] = useState<Filter>('active')
  const [tick, setTick] = useState(0)
  const [modal, setModal] = useState<{ open: boolean; todo: Todo | null }>({
    open: false,
    todo: null,
  })
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<{ title: string; ddl: Dayjs }>()

  useEffect(() => {
    const timer = window.setInterval(() => setTick((t) => t + 1), 1000)
    return () => window.clearInterval(timer)
  }, [])

  const now = useMemo(() => dayjs(), [tick])

  const visible = useMemo(() => {
    const list = [...todos].sort((a, b) => dayjs(a.ddl).valueOf() - dayjs(b.ddl).valueOf())
    if (filter === 'active') return list.filter((t) => !t.completed)
    if (filter === 'completed') return list.filter((t) => t.completed)
    return list
  }, [todos, filter])

  // 循环待办的来源规则标题（规则被删除时返回 null）
  const recurringSource = (id: number) =>
    recurringTodos.find((r) => r.id === id)?.title ?? null

  const stats = useMemo(() => {
    const active = todos.filter((t) => !t.completed)
    const overdue = active.filter((t) => dayjs(t.ddl).isBefore(now))
    const soon = active.filter(
      (t) => !dayjs(t.ddl).isBefore(now) && dayjs(t.ddl).diff(now, 'hour') < 24,
    )
    return { active: active.length, overdue: overdue.length, soon: soon.length }
  }, [todos, now])

  const openModal = (todo: Todo | null) => {
    setModal({ open: true, todo })
    if (todo) {
      form.setFieldsValue({ title: todo.title, ddl: dayjs(todo.ddl) })
    } else {
      form.resetFields()
      form.setFieldsValue({ ddl: dayjs().add(1, 'day').hour(23).minute(59) })
    }
  }

  const submit = async () => {
    const values = await form.validateFields()
    const payload = {
      title: values.title.trim(),
      ddl: values.ddl.format('YYYY-MM-DDTHH:mm:ss'),
    }
    setSaving(true)
    try {
      if (modal.todo) {
        await todoApi.update(modal.todo.id, payload)
        message.success('待办已更新')
      } else {
        await todoApi.create(payload)
        message.success('待办已创建')
      }
      setModal({ open: false, todo: null })
      await refreshTodos()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const toggle = async (todo: Todo) => {
    try {
      await todoApi.toggle(todo.id)
      await refreshTodos()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败')
    }
  }

  const remove = async (todo: Todo) => {
    try {
      await todoApi.remove(todo.id)
      message.success('待办已删除')
      await refreshTodos()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const todoTab = (
    <>
      <div className="page-header">
        <Space size={24} wrap>
          <Statistic title="待完成" value={stats.active} />
          <Statistic
            title="24 小时内到期"
            value={stats.soon}
            valueStyle={{ color: stats.soon > 0 ? '#d46b08' : undefined }}
          />
          <Statistic
            title="已逾期"
            value={stats.overdue}
            valueStyle={{ color: stats.overdue > 0 ? '#cf1322' : undefined }}
          />
        </Space>
        <Space wrap>
          <Segmented
            value={filter}
            onChange={(v) => setFilter(v as Filter)}
            options={[
              { value: 'active', label: '未完成' },
              { value: 'completed', label: '已完成' },
              { value: 'all', label: '全部' },
            ]}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal(null)}>
            新建待办
          </Button>
        </Space>
      </div>

      <Card style={{ marginTop: 16 }} bodyStyle={{ padding: '4px 12px' }}>
        <List
          dataSource={visible}
          locale={{ emptyText: <Empty description="暂无待办" /> }}
          renderItem={(todo) => {
            const ddl = dayjs(todo.ddl)
            const overdue = !todo.completed && ddl.isBefore(now)
            const urgent = !todo.completed && !overdue && ddl.diff(now, 'hour') < 24
            return (
              <List.Item
                actions={[
                  <Button
                    key="edit"
                    type="text"
                    icon={<EditOutlined />}
                    onClick={() => openModal(todo)}
                  />,
                  <Popconfirm
                    key="del"
                    title="确定删除该待办吗？"
                    okText="删除"
                    cancelText="取消"
                    onConfirm={() => remove(todo)}
                  >
                    <Button type="text" danger icon={<DeleteOutlined />} />
                  </Popconfirm>,
                ]}
              >
                <List.Item.Meta
                  avatar={
                    <Checkbox
                      checked={todo.completed}
                      onChange={() => toggle(todo)}
                      style={{ marginTop: 4 }}
                    />
                  }
                  title={
                    <Space>
                      <Typography.Text
                        delete={todo.completed}
                        type={todo.completed ? 'secondary' : undefined}
                      >
                        {todo.title}
                      </Typography.Text>
                      {todo.recurringId !== null && (
                        <Tooltip
                          title={
                            recurringSource(todo.recurringId)
                              ? `来自循环任务：${recurringSource(todo.recurringId)}`
                              : '来自循环任务（规则已删除）'
                          }
                        >
                          <Tag icon={<RetweetOutlined />} color="blue">
                            循环
                          </Tag>
                        </Tooltip>
                      )}
                      {todo.examId !== null && (
                        <Tooltip title="来自考试：ddl=考试开始时间，考试结束后自动完成">
                          <Tag icon={<ScheduleOutlined />} color="red">
                            考试
                          </Tag>
                        </Tooltip>
                      )}
                      {todo.completed ? (
                        <Tag color="green">已完成</Tag>
                      ) : (
                        <Tag color={overdue ? 'red' : urgent ? 'orange' : 'blue'}>
                          {overdue ? '已逾期' : formatCountdown(todo.ddl, now)}
                        </Tag>
                      )}
                    </Space>
                  }
                  description={`截止 ${ddl.format('YYYY-MM-DD HH:mm')}`}
                />
              </List.Item>
            )
          }}
        />
      </Card>
    </>
  )

  return (
    <>
      <Tabs
        items={[
          { key: 'todos', label: '待办事项', children: todoTab },
          {
            key: 'recurring',
            label: `循环任务${recurringTodos.length ? ` (${recurringTodos.length})` : ''}`,
            children: <RecurringTodoPanel />,
          },
        ]}
      />

      <Modal
        open={modal.open}
        maskClosable={false}
        title={modal.todo ? '编辑待办' : '新建待办'}
        onCancel={() => setModal({ open: false, todo: null })}
        onOk={submit}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
        destroyOnClose
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, message: '请输入待办标题' }]}
          >
            <Input placeholder="如 交高数作业" />
          </Form.Item>
          <Form.Item
            name="ddl"
            label="截止时间"
            rules={[{ required: true, message: '请选择截止时间' }]}
          >
            <DatePicker showTime format="YYYY-MM-DD HH:mm" style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  )
}
