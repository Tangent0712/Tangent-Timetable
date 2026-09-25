import { useState } from 'react'
import {
  Button,
  Card,
  Empty,
  Popconfirm,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { DeleteOutlined, EditOutlined, PlusOutlined, ThunderboltOutlined } from '@ant-design/icons'
import RecurringFormModal from './RecurringFormModal'
import { recurringApi } from '../api'
import { useApp } from '../store/AppContext'
import type { RecurringTodo, RecurringTodoPayload } from '../types'
import { dayjs } from '../utils/schedule'
import { describeFrequency, describeOffset } from '../utils/recurring'

export default function RecurringTodoPanel() {
  const { recurringTodos, refreshRecurring, refreshTodos } = useApp()
  const [modal, setModal] = useState<{ open: boolean; rule: RecurringTodo | null }>({
    open: false,
    rule: null,
  })
  const [saving, setSaving] = useState(false)

  const submit = async (payload: RecurringTodoPayload) => {
    setSaving(true)
    try {
      if (modal.rule) {
        await recurringApi.update(modal.rule.id, payload)
        message.success('循环任务已更新')
      } else {
        await recurringApi.create(payload)
        message.success('循环任务已创建')
      }
      setModal({ open: false, rule: null })
      await refreshRecurring()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const toggle = async (rule: RecurringTodo) => {
    try {
      await recurringApi.toggle(rule.id)
      await refreshRecurring()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '操作失败')
    }
  }

  const remove = async (rule: RecurringTodo) => {
    try {
      await recurringApi.remove(rule.id)
      message.success('循环任务已删除，已生成的待办保留')
      await Promise.all([refreshRecurring(), refreshTodos()])
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const triggerNow = async (rule: RecurringTodo) => {
    try {
      await recurringApi.triggerNow(rule.id)
      message.success('已立即生成一条待办')
      await Promise.all([refreshRecurring(), refreshTodos()])
    } catch (e) {
      message.error(e instanceof Error ? e.message : '生成失败')
    }
  }

  const columns: ColumnsType<RecurringTodo> = [
    {
      title: '标题',
      dataIndex: 'title',
      render: (title: string, rule) => (
        <Space size={6}>
          <Typography.Text strong={rule.enabled} type={rule.enabled ? undefined : 'secondary'}>
            {title}
          </Typography.Text>
          {!rule.enabled && <Tag>已停用</Tag>}
          {rule.frequency === 'CUSTOM' && <Tag color="geekblue">自定义</Tag>}
          {rule.chainAfterComplete && (
            <Tooltip title="完成当期后立即生成下一期">
              <Tag color="purple">连续</Tag>
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: '触发规则',
      width: 160,
      render: (_, rule) => describeFrequency(rule),
    },
    {
      title: '截止',
      width: 140,
      render: (_, rule) => (
        <Typography.Text type="secondary">
          触发后 {describeOffset(rule.ddlOffsetMinutes)}
        </Typography.Text>
      ),
    },
    {
      title: '下次触发',
      width: 170,
      render: (_, rule) =>
        rule.enabled && rule.nextTriggerAt ? (
          <Space size={4} direction="vertical" style={{ lineHeight: 1.3 }}>
            <span>{dayjs(rule.nextTriggerAt).format('MM-DD HH:mm')}</span>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {dayjs(rule.nextTriggerAt).fromNow()}
            </Typography.Text>
          </Space>
        ) : (
          <span className="text-muted">—</span>
        ),
    },
    {
      title: '启用',
      width: 70,
      render: (_, rule) => (
        <Switch size="small" checked={rule.enabled} onChange={() => toggle(rule)} />
      ),
    },
    {
      title: '操作',
      width: 120,
      render: (_, rule) => (
        <Space size={0}>
          <Tooltip title="立即生成一条待办">
            <Button type="text" icon={<ThunderboltOutlined />} onClick={() => triggerNow(rule)} />
          </Tooltip>
          <Button
            type="text"
            icon={<EditOutlined />}
            onClick={() => setModal({ open: true, rule })}
          />
          <Popconfirm
            title="删除该循环任务？"
            description="已生成的待办会保留"
            okText="删除"
            cancelText="取消"
            onConfirm={() => remove(rule)}
          >
            <Button type="text" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <>
      <Card
        size="small"
        title={`循环任务（${recurringTodos.length}）`}
        extra={
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => setModal({ open: true, rule: null })}
          >
            新建循环任务
          </Button>
        }
      >
        <Table
          size="small"
          rowKey="id"
          columns={columns}
          dataSource={recurringTodos}
          pagination={false}
          scroll={{ x: 800 }}
          locale={{
            emptyText: (
              <Empty
                description={
                  <Space direction="vertical" size={2}>
                    <span>还没有循环任务</span>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      例如：每周五 12:00 提醒「刷本周网课」，本周日 12:00 截止
                    </Typography.Text>
                  </Space>
                }
              />
            ),
          }}
        />
      </Card>

      <RecurringFormModal
        open={modal.open}
        rule={modal.rule}
        saving={saving}
        onCancel={() => setModal({ open: false, rule: null })}
        onSubmit={submit}
      />
    </>
  )
}
