import { useMemo } from 'react'
import { Empty, Tooltip } from 'antd'
import { WarningFilled } from '@ant-design/icons'
import type { Course, PeriodConfig } from '../types'
import { courseColor } from '../utils/color'
import { WEEK_DAY_NAMES, dateOfWeekDay, dayjs, formatWeeks } from '../utils/schedule'

interface Props {
  courses: Course[]
  periods: PeriodConfig[]
  week: number
  semesterStart: string | null
  showAllWeeks: boolean
  onCourseClick: (course: Course) => void
  onEmptyClick: (dayOfWeek: number, period: number) => void
}

interface Placed {
  /** 实际展示（生效）的课程 */
  course: Course
  span: number
  /** 与之冲突、被隐藏的课程 */
  conflicts: Course[]
}

/** 判断两门课的时间段是否重叠 */
function overlaps(a: Course, b: Course): boolean {
  return (
    a.dayOfWeek === b.dayOfWeek &&
    a.startPeriod <= b.endPeriod &&
    b.startPeriod <= a.endPeriod
  )
}

/**
 * 冲突处理：同一时段重叠的课程不并排显示（那会把后面的课挤走），
 * 而是选定其中一门作为「生效课程」展示，其余记录为冲突项，
 * 在该格子上以角标提示，点击可查看全部冲突课程。
 *
 * 选取规则：先按开始节次，再按课程 id，保证稳定且可预测。
 */
function resolveConflicts(courses: Course[]): Placed[] {
  const sorted = [...courses].sort(
    (a, b) => a.startPeriod - b.startPeriod || a.id - b.id,
  )
  const result: Placed[] = []

  for (const course of sorted) {
    const existing = result.find((p) => overlaps(p.course, course))
    if (existing) {
      existing.conflicts.push(course)
    } else {
      result.push({
        course,
        span: Math.max(1, course.endPeriod - course.startPeriod + 1),
        conflicts: [],
      })
    }
  }
  return result
}

export default function TimetableGrid({
  courses,
  periods,
  week,
  semesterStart,
  showAllWeeks,
  onCourseClick,
  onEmptyClick,
}: Props) {
  const periodList = useMemo(
    () => [...periods].sort((a, b) => a.periodNumber - b.periodNumber),
    [periods],
  )

  const { cellMap, occupied } = useMemo(() => {
    const map = new Map<string, Placed>()
    const taken = new Set<string>()

    const visible = courses.filter((c) => showAllWeeks || c.weeks?.includes(week))

    // 按天分组后逐天解决冲突
    for (let day = 1; day <= 7; day++) {
      const dayCourses = visible.filter((c) => c.dayOfWeek === day)
      for (const placed of resolveConflicts(dayCourses)) {
        map.set(`${day}-${placed.course.startPeriod}`, placed)
        for (let p = placed.course.startPeriod + 1; p <= placed.course.endPeriod; p++) {
          taken.add(`${day}-${p}`)
        }
        // 被隐藏的冲突课程占用的格子也要标记，避免出现空洞可点击
        for (const c of placed.conflicts) {
          for (let p = c.startPeriod; p <= c.endPeriod; p++) {
            if (p !== placed.course.startPeriod) taken.add(`${day}-${p}`)
          }
        }
      }
    }
    return { cellMap: map, occupied: taken }
  }, [courses, showAllWeeks, week])

  /**
   * 时段分界线：上午 / 下午 / 晚上 之间加粗分隔。
   * 收集每个 category 最后一节的节次号（最末一节不需要，因为表格自带外边框）。
   */
  const dividerPeriods = useMemo(() => {
    const set = new Set<number>()
    for (let i = 0; i < periodList.length - 1; i++) {
      if (periodList[i].category !== periodList[i + 1].category) {
        set.add(periodList[i].periodNumber)
      }
    }
    return set
  }, [periodList])

  if (periodList.length === 0) {
    return <Empty description="尚未配置作息时间表" />
  }

  const today = dayjs().startOf('day')

  const headers = WEEK_DAY_NAMES.map((name, index) => {
    const dayOfWeek = index + 1
    const date = semesterStart ? dateOfWeekDay(semesterStart, week, dayOfWeek) : null
    const isToday = date !== null && date.startOf('day').isSame(today)
    return (
      <div
        key={`head-${dayOfWeek}`}
        className={`tt-cell tt-head${isToday ? ' tt-head-today' : ''}`}
      >
        <span>{name}</span>
        {date && <span className="tt-head-sub">{date.format('MM-DD')}</span>}
      </div>
    )
  })

  const rows = periodList.flatMap((period) => {
    // 该行是否为时段分界行（上午/下午/晚上交界）
    const isDivider = dividerPeriods.has(period.periodNumber)
    const div = isDivider ? ' tt-divider' : ''

    const cells = [
      <div key={`p-${period.periodNumber}`} className={`tt-cell tt-period${div}`}>
        <span className="tt-period-num">{period.periodNumber}</span>
        <span>{period.startTime?.slice(0, 5)}</span>
        <span>{period.endTime?.slice(0, 5)}</span>
      </div>,
    ]

    for (let day = 1; day <= 7; day++) {
      const key = `${day}-${period.periodNumber}`
      const placed = cellMap.get(key)

      if (placed) {
        const { course, span, conflicts } = placed
        const color = courseColor(course.name)
        const hasConflict = conflicts.length > 0
        // 跨节课程按其结束节次判断是否压在分界线上
        const spanDiv = dividerPeriods.has(course.endPeriod) ? ' tt-divider' : ''
        cells.push(
          <div
            key={`c-${key}`}
            className={`tt-cell${spanDiv}`}
            style={{ gridRow: `span ${span}` }}
          >
            <Tooltip
              title={
                <div>
                  <div>{course.name}</div>
                  <div>地点：{course.location || '未指定'}</div>
                  <div>教师：{course.teacher || '未指定'}</div>
                  <div>
                    第 {course.startPeriod}-{course.endPeriod} 节
                  </div>
                  <div>周次：{formatWeeks(course.weeks)}</div>
                  {hasConflict && (
                    <div style={{ marginTop: 6, borderTop: '1px solid #595959', paddingTop: 6 }}>
                      <div>⚠ 此时段有 {conflicts.length} 门课冲突（未显示）：</div>
                      {conflicts.map((c) => (
                        <div key={c.id}>
                          · {c.name}（第 {c.startPeriod}-{c.endPeriod} 节
                          {c.location ? `，${c.location}` : ''}）
                        </div>
                      ))}
                      <div style={{ marginTop: 4 }}>以上课程不会显示在课表中，请调整或删除。</div>
                    </div>
                  )}
                </div>
              }
            >
              <div
                className="course-block"
                style={{
                  background: color.bg,
                  borderLeftColor: hasConflict ? '#ff4d4f' : color.border,
                  color: color.text,
                  height: '100%',
                }}
                onClick={() => onCourseClick(course)}
              >
                {hasConflict && (
                  <span className="course-conflict-badge">
                    <WarningFilled /> {conflicts.length}
                  </span>
                )}
                <div className="course-block-name">{course.name}</div>
                {course.location && (
                  <div className="course-block-meta">{course.location}</div>
                )}
                {course.teacher && (
                  <div className="course-block-meta">{course.teacher}</div>
                )}
              </div>
            </Tooltip>
          </div>,
        )
      } else if (!occupied.has(key)) {
        cells.push(
          <div
            key={`e-${key}`}
            className={`tt-cell tt-cell-empty${div}`}
            onClick={() => onEmptyClick(day, period.periodNumber)}
          />,
        )
      }
    }
    return cells
  })

  return (
    <div className="timetable-grid">
      <div className="tt-cell tt-head">节次</div>
      {headers}
      {rows}
    </div>
  )
}
