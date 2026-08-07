import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  Empty,
  Input,
  List,
  Popconfirm,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from 'antd'
import { DeleteOutlined, PlusOutlined, SendOutlined } from '@ant-design/icons'
import AiActionCard from '../components/AiActionCard'
import { aiApi } from '../api'
import { useApp } from '../store/AppContext'
import type { AiConversation, AiMessage } from '../types'
import { formatWeekInfo } from '../utils/schedule'

const EXAMPLES = [
  '下周一3-4节加一节形势政策，地点待定',
  '把周三的高数从第3-4节改到第5-6节',
  '删掉周五上午的课，再加一条周三交实验报告的待办',
  '下周五下午5点前交高数作业',
]

/** 单个对话的流式进行状态 */
interface StreamState {
  phase: 'thinking' | 'text' | 'planning'
  text: string
  userText: string
}

export default function AiChatPage() {
  const { activeSchedule, activeScheduleId, courses, todos, refreshCourses, refreshTodos } =
    useApp()

  const [enabled, setEnabled] = useState<boolean | null>(null)
  const [conversations, setConversations] = useState<AiConversation[]>([])
  const [currentId, setCurrentId] = useState<number | null>(null)
  const [messages, setMessages] = useState<AiMessage[]>([])
  const [input, setInput] = useState('')
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [executingId, setExecutingId] = useState<number | null>(null)
  // 关键：流式状态按 conversationId 存储，避免串台到其他对话
  const [streams, setStreams] = useState<Record<number, StreamState>>({})
  const bottomRef = useRef<HTMLDivElement>(null)

  const stream = currentId !== null ? streams[currentId] : undefined
  const sending = stream !== undefined

  const patchStream = useCallback((convId: number, patch: Partial<StreamState>) => {
    setStreams((prev) => {
      const cur = prev[convId]
      if (!cur) return prev
      return { ...prev, [convId]: { ...cur, ...patch } }
    })
  }, [])

  const clearStream = useCallback((convId: number) => {
    setStreams((prev) => {
      const next = { ...prev }
      delete next[convId]
      return next
    })
  }, [])

  useEffect(() => {
    aiApi
      .status()
      .then((s) => setEnabled(s.enabled))
      .catch(() => setEnabled(false))
  }, [])

  const loadConversations = useCallback(async () => {
    const list = await aiApi.listConversations()
    setConversations(list)
    return list
  }, [])

  useEffect(() => {
    loadConversations()
      .then((list) => {
        if (list.length > 0) setCurrentId(list[0].id)
      })
      .catch((e: Error) => message.error(e.message))
  }, [loadConversations])

  useEffect(() => {
    if (currentId === null) {
      setMessages([])
      return
    }
    setLoadingMessages(true)
    aiApi
      .listMessages(currentId)
      .then(setMessages)
      .catch((e: Error) => message.error(e.message))
      .finally(() => setLoadingMessages(false))
  }, [currentId])

  // 课表/待办变化时重新拉取消息，让失效提案立即显示
  const dataVersion = `${courses.length}-${todos.length}-${courses
    .map((c) => c.updatedAt ?? c.id)
    .join(',')}-${todos.map((t) => t.updatedAt ?? t.id).join(',')}`
  const skipFirstRef = useRef(true)
  useEffect(() => {
    if (skipFirstRef.current) {
      skipFirstRef.current = false
      return
    }
    if (currentId === null || sending) return
    aiApi
      .listMessages(currentId)
      .then(setMessages)
      .catch(() => undefined)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dataVersion])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, stream?.text, stream?.phase])

  const newConversation = async () => {
    try {
      const conv = await aiApi.createConversation(activeScheduleId)
      await loadConversations()
      setCurrentId(conv.id)
      setMessages([])
    } catch (e) {
      message.error(e instanceof Error ? e.message : '创建对话失败')
    }
  }

  const deleteConversation = async (id: number) => {
    try {
      await aiApi.deleteConversation(id)
      clearStream(id)
      const list = await loadConversations()
      if (currentId === id) setCurrentId(list.length > 0 ? list[0].id : null)
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const rename = async (id: number, title: string) => {
    try {
      await aiApi.renameConversation(id, title)
      await loadConversations()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '重命名失败')
    }
  }

  const send = async () => {
    const content = input.trim()
    if (!content || sending) return

    let convId = currentId
    setInput('')

    try {
      if (convId === null) {
        const conv = await aiApi.createConversation(activeScheduleId)
        convId = conv.id
        setCurrentId(conv.id)
        await loadConversations()
      }
    } catch (e) {
      message.error(e instanceof Error ? e.message : '创建对话失败')
      setInput(content)
      return
    }

    const cid = convId
    setStreams((prev) => ({
      ...prev,
      [cid]: { phase: 'thinking', text: '', userText: content },
    }))

    try {
      await aiApi.sendMessageStream(cid, content, activeScheduleId, {
        onThinking: () => patchStream(cid, { phase: 'thinking' }),
        onText: (delta) =>
          setStreams((prev) => {
            const cur = prev[cid]
            if (!cur) return prev
            return { ...prev, [cid]: { ...cur, phase: 'text', text: cur.text + delta } }
          }),
        onPlanning: () => patchStream(cid, { phase: 'planning' }),
      })
      const all = await aiApi.listMessages(cid)
      clearStream(cid)
      // 仅当用户仍停留在该对话时才更新消息区
      setCurrentId((active) => {
        if (active === cid) setMessages(all)
        return active
      })
      await loadConversations()
    } catch (e) {
      clearStream(cid)
      message.error(e instanceof Error ? e.message : '发送失败')
      try {
        const all = await aiApi.listMessages(cid)
        setMessages(all)
      } catch {
        // 忽略
      }
      setInput(content)
    }
  }

  const execute = async (actionId: number) => {
    if (currentId === null) return
    setExecutingId(actionId)
    try {
      await aiApi.execute(currentId, actionId)
      message.success('操作已执行')
      setMessages(await aiApi.listMessages(currentId))
      await Promise.all([refreshCourses(), refreshTodos()])
    } catch (e) {
      message.error(e instanceof Error ? e.message : '执行失败')
      try {
        setMessages(await aiApi.listMessages(currentId))
      } catch {
        // 忽略
      }
    } finally {
      setExecutingId(null)
    }
  }

  const reject = async (actionId: number) => {
    if (currentId === null) return
    try {
      await aiApi.reject(currentId, actionId)
      setMessages(await aiApi.listMessages(currentId))
    } catch (e) {
      message.error(e instanceof Error ? e.message : '取消失败')
    }
  }

  const weekInfo = activeSchedule
    ? formatWeekInfo(activeSchedule.periodStartDate, activeSchedule.periodEndDate)
    : null

  return (
    <div style={{ display: 'flex', gap: 16, alignItems: 'flex-start' }}>
      <Card
        size="small"
        title="对话"
        style={{ width: 240, flexShrink: 0 }}
        extra={
          <Button
            size="small"
            type="text"
            icon={<PlusOutlined />}
            onClick={newConversation}
          />
        }
      >
        <List
          size="small"
          dataSource={conversations}
          locale={{ emptyText: '暂无对话' }}
          renderItem={(conv) => (
            <List.Item
              style={{
                cursor: 'pointer',
                background: conv.id === currentId ? '#e6f4ff' : undefined,
                borderRadius: 6,
                paddingInline: 8,
              }}
              onClick={() => setCurrentId(conv.id)}
              actions={[
                <Popconfirm
                  key="del"
                  title="删除该对话？"
                  okText="删除"
                  cancelText="取消"
                  onConfirm={() => deleteConversation(conv.id)}
                >
                  <Button
                    size="small"
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    onClick={(e) => e.stopPropagation()}
                  />
                </Popconfirm>,
              ]}
            >
              <Space size={4}>
                <Typography.Text
                  ellipsis
                  editable={{
                    onChange: (v) => {
                      const t = v.trim()
                      if (t && t !== conv.title) rename(conv.id, t)
                    },
                    tooltip: '点击重命名',
                    triggerType: ['icon'],
                  }}
                  style={{ maxWidth: 110 }}
                >
                  {conv.title || '新对话'}
                </Typography.Text>
                {streams[conv.id] && <Spin size="small" />}
              </Space>
            </List.Item>
          )}
        />
      </Card>

      <Card
        style={{ flex: 1, minWidth: 0 }}
        title={
          <Space wrap>
            <span>AI 助手</span>
            {activeSchedule ? (
              <>
                <Tag color="blue">{activeSchedule.name}</Tag>
                {weekInfo && <Tag>{weekInfo}</Tag>}
              </>
            ) : (
              <Tag color="orange">未选择课表</Tag>
            )}
          </Space>
        }
      >
        {enabled === false && (
          <Alert
            type="warning"
            showIcon
            message="AI 功能未启用"
            description="后端未配置 DEEPSEEK_API_KEY 环境变量，请设置后重启服务。"
            style={{ marginBottom: 16 }}
          />
        )}

        <div className="chat-layout">
          <div className="chat-messages">
            {loadingMessages ? (
              <div style={{ textAlign: 'center', padding: 40 }}>
                <Spin />
              </div>
            ) : messages.length === 0 && !stream ? (
              <Empty
                description={
                  <Space direction="vertical" align="center">
                    <span>用一句话管理课表和待办，支持一次做多件事</span>
                    <Space wrap style={{ justifyContent: 'center' }}>
                      {EXAMPLES.map((ex) => (
                        <Button key={ex} size="small" onClick={() => setInput(ex)}>
                          {ex}
                        </Button>
                      ))}
                    </Space>
                  </Space>
                }
                style={{ paddingTop: 60 }}
              />
            ) : (
              messages.map((msg) => (
                <div
                  key={msg.messageId}
                  className={`chat-row${msg.role === 'user' ? ' chat-row-user' : ''}`}
                >
                  <div>
                    <div
                      className={`chat-bubble chat-bubble-${
                        msg.role === 'user' ? 'user' : 'assistant'
                      }`}
                    >
                      {msg.text}
                    </div>
                    {(msg.actions ?? []).map((action) => (
                      <AiActionCard
                        key={action.actionId}
                        action={action}
                        executing={executingId === action.actionId}
                        onExecute={() => execute(action.actionId)}
                        onReject={() => reject(action.actionId)}
                      />
                    ))}
                  </div>
                </div>
              ))
            )}

            {stream && (
              <>
                <div className="chat-row chat-row-user">
                  <div className="chat-bubble chat-bubble-user">{stream.userText}</div>
                </div>
                <div className="chat-row">
                  <div className="chat-bubble chat-bubble-assistant">
                    {stream.phase === 'thinking' && (
                      <Space size={6}>
                        <Spin size="small" />
                        <Typography.Text type="secondary">思考中…</Typography.Text>
                      </Space>
                    )}
                    {stream.phase !== 'thinking' && (
                      <>
                        {stream.text}
                        {stream.phase === 'planning' && (
                          <div style={{ marginTop: 8 }}>
                            <Space size={6}>
                              <Spin size="small" />
                              <Typography.Text type="secondary">规划操作中…</Typography.Text>
                            </Space>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
              </>
            )}
            <div ref={bottomRef} />
          </div>

          <div className="chat-input-bar">
            <Input.TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="输入自然语言指令，可一次说多件事（Enter 发送，Shift+Enter 换行）"
              autoSize={{ minRows: 1, maxRows: 4 }}
              disabled={enabled === false}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault()
                  if (!sending) send()
                }
              }}
            />
            <Button
              type="primary"
              icon={<SendOutlined />}
              loading={sending}
              disabled={enabled === false || !input.trim()}
              onClick={send}
            >
              发送
            </Button>
          </div>
        </div>
      </Card>
    </div>
  )
}
