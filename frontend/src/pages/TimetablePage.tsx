import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Button,
  Card,
  Empty,
  Radio,
  Segmented,
  Slider,
  Space,
  Typography,
  message,
} from 'antd'
import {
  ImportOutlined,
  LeftOutlined,
  PlusOutlined,
  RightOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import TimetableGrid from '../components/TimetableGrid'
import CourseListView from '../components/CourseListView'
import CourseFormModal from '../components/CourseFormModal'
import ScheduleManagerModal from '../components/ScheduleManagerModal'
import ImportHtmlModal from '../components/ImportHtmlModal'
import { aiApi, courseApi, scheduleApi } from '../api'
import { useApp } from '../store/AppContext'
import type { Course, CoursePayload, Schedule } from '../types'
import { currentWeekOf, totalWeeks } from '../utils/schedule'

export default function TimetablePage() {
  const {
    activeSchedule,
    activeScheduleId,
    schedules,
    courses,
    periods,
    refreshCourses,
    refreshSchedules,
  } = useApp()

  const [week, setWeek] = useState(1)
  const [viewMode, setViewMode] = useState<'week' | 'all'>('week')
  const [courseModal, setCourseModal] = useState<{
    open: boolean
    course: Course | null
    defaults: { dayOfWeek?: number; startPeriod?: number } | null
  }>({ open: false, course: null, defaults: null })
  const [saving, setSaving] = useState(false)
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false)
  const [importOpen, setImportOpen] = useState(false)
  const [aiEnabled, setAiEnabled] = useState(false)

  const total = useMemo(
    () =>
      activeSchedule
        ? totalWeeks(activeSchedule.periodStartDate, activeSchedule.periodEndDate)
        : 20,
    [activeSchedule],
  )

  const realCurrentWeek = useMemo(() => {
    if (!activeSchedule) return 1
    return Math.min(Math.max(currentWeekOf(activeSchedule.periodStartDate), 1), total)
  }, [activeSchedule, total])

  useEffect(() => {
    setWeek(realCurrentWeek)
  }, [realCurrentWeek])

  useEffect(() => {
    aiApi
      .status()
      .then((s) => setAiEnabled(s.enabled))
      .catch(() => setAiEnabled(false))
  }, [])

  const openCreate = useCallback(
    (dayOfWeek?: number, startPeriod?: number) => {
      if (!activeScheduleId) {
        message.warning('请先创建或选择一个课表')
        return
      }
      setCourseModal({ open: true, course: null, defaults: { dayOfWeek, startPeriod } })
    },
    [activeScheduleId],
  )

  const submitCourse = async (payload: CoursePayload) => {
    if (!activeScheduleId) return
    setSaving(true)
    try {
      if (courseModal.course) {
        await courseApi.update(courseModal.course.id, payload)
        message.success('课程已更新')
      } else {
        await courseApi.create(activeScheduleId, payload)
        message.success('课程已添加')
      }
      setCourseModal({ open: false, course: null, defaults: null })
      await refreshCourses()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const deleteCourse = async (course: Course) => {
    try {
      await courseApi.remove(course.id)
      message.success('课程已删除')
      setCourseModal({ open: false, course: null, defaults: null })
      await refreshCourses()
    } catch (e) {
      message.error(e instanceof Error ? e.message : '删除失败')
    }
  }

  const createSchedule = async (payload: Omit<Schedule, 'id'>) => {
    await scheduleApi.create(payload)
    message.success('课表已创建')
    await refreshSchedules()
  }

  const updateSchedule = async (id: number, payload: Omit<Schedule, 'id'>) => {
    await scheduleApi.update(id, payload)
    message.success('课表已更新')
    await refreshSchedules()
  }

  const deleteSchedule = async (id: number) => {
    await scheduleApi.remove(id)
    message.success('课表已删除')
    await refreshSchedules()
    await refreshCourses()
  }

  if (schedules.length === 0) {
    return (
      <Card>
        <Empty description="还没有课表，先创建一个学期课表吧">
          <Space>
            <Button type="primary" onClick={() => setScheduleModalOpen(true)}>
              新建课表
            </Button>
          </Space>
        </Empty>
        <ScheduleManagerModal
          open={scheduleModalOpen}
          schedules={schedules}
          onCancel={() => setScheduleModalOpen(false)}
          onCreate={createSchedule}
          onUpdate={updateSchedule}
          onDelete={deleteSchedule}
        />
      </Card>
    )
  }

  return (
    <>
      <div className="page-header">
        <Space size={12} wrap>
          <Typography.Title level={4} style={{ margin: 0 }}>
            {activeSchedule?.name ?? '课表'}
          </Typography.Title>
          <Segmented
            value={viewMode}
            onChange={(v) => setViewMode(v as 'week' | 'all')}
            options={[
              { value: 'week', label: '按周' },
              { value: 'all', label: '全部课程' },
            ]}
          />
          {viewMode === 'week' && (
            <Space size={4}>
              <Button
                size="small"
                icon={<LeftOutlined />}
                disabled={week <= 1}
                onClick={() => setWeek((w) => Math.max(1, w - 1))}
              />
              <Typography.Text strong style={{ minWidth: 68, textAlign: 'center' }}>
                第 {week} 周
              </Typography.Text>
              <Button
                size="small"
                icon={<RightOutlined />}
                disabled={week >= total}
                onClick={() => setWeek((w) => Math.min(total, w + 1))}
              />
              <Radio.Group
                size="small"
                value={week === realCurrentWeek ? 'now' : ''}
                onChange={() => setWeek(realCurrentWeek)}
              >
                <Radio.Button value="now">本周</Radio.Button>
              </Radio.Group>
            </Space>
          )}
        </Space>

        <Space wrap>
          <Button icon={<PlusOutlined />} type="primary" onClick={() => openCreate()}>
            添加课程
          </Button>
          <Button icon={<ImportOutlined />} onClick={() => setImportOpen(true)}>
            导入课表
          </Button>
          <Button icon={<SettingOutlined />} onClick={() => setScheduleModalOpen(true)}>
            课表管理
          </Button>
        </Space>
      </div>

      {viewMode === 'week' && total > 1 && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Slider
            min={1}
            max={total}
            value={week}
            onChange={setWeek}
            marks={{ 1: '1', [realCurrentWeek]: '本周', [total]: String(total) }}
          />
        </Card>
      )}

      {viewMode === 'week' ? (
        <TimetableGrid
          courses={courses}
          periods={periods}
          week={week}
          semesterStart={activeSchedule?.periodStartDate ?? null}
          onCourseClick={(course) =>
            setCourseModal({ open: true, course, defaults: null })
          }
          onEmptyClick={(day, period) => openCreate(day, period)}
        />
      ) : (
        <CourseListView
          courses={courses}
          periods={periods}
          onEdit={(course) => setCourseModal({ open: true, course, defaults: null })}
          onDelete={deleteCourse}
        />
      )}

      <CourseFormModal
        open={courseModal.open}
        course={courseModal.course}
        defaults={courseModal.defaults}
        periods={periods}
        totalWeeks={total}
        currentWeek={realCurrentWeek}
        saving={saving}
        onCancel={() => setCourseModal({ open: false, course: null, defaults: null })}
        onSubmit={submitCourse}
        onDelete={deleteCourse}
      />

      <ScheduleManagerModal
        open={scheduleModalOpen}
        schedules={schedules}
        onCancel={() => setScheduleModalOpen(false)}
        onCreate={createSchedule}
        onUpdate={updateSchedule}
        onDelete={deleteSchedule}
      />

      <ImportHtmlModal
        open={importOpen}
        aiEnabled={aiEnabled}
        onCancel={() => setImportOpen(false)}
        onParse={async (html) => {
          const res = await aiApi.parseHtml(html, activeScheduleId)
          return res.courses
        }}
        onImport={async (list) => {
          if (!activeScheduleId) throw new Error('请先选择课表')
          await courseApi.batchCreate(activeScheduleId, list)
          await refreshCourses()
        }}
      />
    </>
  )
}
