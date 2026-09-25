import { useState } from 'react'
import { Alert, Button, Input, Modal, Result, Space, Steps, Table, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { CoursePayload, ParsedCourse } from '../types'
import { formatWeeks, weekDayName } from '../utils/schedule'

interface Props {
  open: boolean
  aiEnabled: boolean
  onCancel: () => void
  onParse: (html: string) => Promise<ParsedCourse[]>
  onImport: (courses: CoursePayload[]) => Promise<void>
}

interface Row extends ParsedCourse {
  key: number
}

export default function ImportHtmlModal({ open, aiEnabled, onCancel, onParse, onImport }: Props) {
  const [step, setStep] = useState(0)
  const [html, setHtml] = useState('')
  const [rows, setRows] = useState<Row[]>([])
  const [selectedKeys, setSelectedKeys] = useState<number[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const reset = () => {
    setStep(0)
    setHtml('')
    setRows([])
    setSelectedKeys([])
    setError(null)
    setLoading(false)
  }

  const close = () => {
    reset()
    onCancel()
  }

  const doParse = async () => {
    setLoading(true)
    setError(null)
    try {
      const parsed = await onParse(html)
      if (parsed.length === 0) {
        setError('AI 没能从内容中解析出课程，请检查粘贴的内容是否完整')
        return
      }
      const next = parsed.map((c, i) => ({ ...c, key: i }))
      setRows(next)
      setSelectedKeys(next.filter(isValid).map((r) => r.key))
      setStep(1)
    } catch (e) {
      setError(e instanceof Error ? e.message : '解析失败')
    } finally {
      setLoading(false)
    }
  }

  const doImport = async () => {
    const picked = rows.filter((r) => selectedKeys.includes(r.key) && isValid(r))
    if (picked.length === 0) {
      setError('请至少选择一条有效课程')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await onImport(
        picked.map((r) => ({
          name: r.name as string,
          location: r.location ?? null,
          teacher: r.teacher ?? null,
          dayOfWeek: r.dayOfWeek as number,
          startPeriod: r.startPeriod as number,
          endPeriod: r.endPeriod as number,
          weeks: r.weeks,
        })),
      )
      setStep(2)
    } catch (e) {
      setError(e instanceof Error ? e.message : '导入失败')
    } finally {
      setLoading(false)
    }
  }

  const columns: ColumnsType<Row> = [
    {
      title: '课程名',
      dataIndex: 'name',
      render: (v: string | null) => v ?? <span className="text-muted">缺失</span>,
    },
    {
      title: '地点',
      dataIndex: 'location',
      render: (v: string | null) => v ?? <span className="text-muted">未指定</span>,
    },
    {
      title: '教师',
      dataIndex: 'teacher',
      render: (v: string | null) => v ?? <span className="text-muted">未指定</span>,
    },
    {
      title: '星期',
      dataIndex: 'dayOfWeek',
      render: (v: number | null) => (v ? weekDayName(v) : <span className="text-muted">缺失</span>),
    },
    {
      title: '节次',
      render: (_, r) =>
        r.startPeriod && r.endPeriod ? (
          `${r.startPeriod}-${r.endPeriod}`
        ) : (
          <span className="text-muted">缺失</span>
        ),
    },
    { title: '周次', dataIndex: 'weeks', render: (v: number[]) => formatWeeks(v) },
  ]

  return (
    <Modal
      open={open}
      maskClosable={false}
      title="从教务系统导入课表"
      onCancel={close}
      width={960}
      footer={null}
      destroyOnHidden
    >
      <Steps
        current={step}
        size="small"
        items={[{ title: '粘贴内容' }, { title: '确认预览' }, { title: '完成' }]}
        style={{ marginBottom: 16 }}
      />

      {!aiEnabled && (
        <Alert
          type="warning"
          showIcon
          message="AI 功能未启用"
          description="后端未配置 DEEPSEEK_API_KEY，导入功能不可用。"
          style={{ marginBottom: 16 }}
        />
      )}

      {error && <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />}

      {step === 0 && (
        <>
          <Typography.Paragraph type="secondary">
            在你自己浏览器里登录教务系统并打开课程表页面，按 Ctrl+A
            全选页面后复制（或查看网页源码全选复制），粘贴到下方，AI 会自动解析成课程列表。
          </Typography.Paragraph>
          <Input.TextArea
            rows={12}
            value={html}
            onChange={(e) => setHtml(e.target.value)}
            placeholder="在此粘贴教务系统课表页面的 HTML 或文本…"
          />
          <Space style={{ marginTop: 16, justifyContent: 'flex-end', width: '100%' }}>
            <Button onClick={close}>取消</Button>
            <Button
              type="primary"
              loading={loading}
              disabled={!aiEnabled || html.trim().length < 10}
              onClick={doParse}
            >
              AI 解析
            </Button>
          </Space>
        </>
      )}

      {step === 1 && (
        <>
          <Typography.Paragraph type="secondary">
            共解析出 {rows.length}{' '}
            条课程，勾选需要导入的项。字段缺失的行无法导入，可在导入后手动编辑补全。
          </Typography.Paragraph>
          <Table
            size="small"
            rowKey="key"
            columns={columns}
            dataSource={rows}
            pagination={false}
            scroll={{ y: 380 }}
            rowSelection={{
              selectedRowKeys: selectedKeys,
              onChange: (keys) => setSelectedKeys(keys as number[]),
              getCheckboxProps: (r) => ({ disabled: !isValid(r) }),
            }}
          />
          <Space style={{ marginTop: 16, justifyContent: 'flex-end', width: '100%' }}>
            <Button onClick={() => setStep(0)}>上一步</Button>
            <Button type="primary" loading={loading} onClick={doImport}>
              确认导入 {selectedKeys.length} 条
            </Button>
          </Space>
        </>
      )}

      {step === 2 && (
        <Result
          status="success"
          title="导入完成"
          subTitle={`已成功导入 ${selectedKeys.length} 条课程`}
          extra={
            <Button type="primary" onClick={close}>
              返回课表
            </Button>
          }
        />
      )}
    </Modal>
  )
}

function isValid(row: ParsedCourse): boolean {
  return !!(
    row.name &&
    row.dayOfWeek &&
    row.startPeriod &&
    row.endPeriod &&
    row.weeks &&
    row.weeks.length > 0
  )
}
