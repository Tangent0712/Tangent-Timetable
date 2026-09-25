package com.timetable.service.impl;

import com.timetable.entity.Course;
import com.timetable.entity.PeriodConfig;
import com.timetable.entity.Schedule;
import com.timetable.entity.Todo;
import com.timetable.service.CourseService;
import com.timetable.service.ExamService;
import com.timetable.service.PeriodConfigService;
import com.timetable.service.ScheduleService;
import com.timetable.service.TodoService;
import com.timetable.service.WidgetService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WidgetServiceImpl implements WidgetService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");
    private static final String[] WEEK_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final ScheduleService scheduleService;
    private final CourseService courseService;
    private final TodoService todoService;
    private final ExamService examService;
    private final PeriodConfigService periodConfigService;

    public WidgetServiceImpl(ScheduleService scheduleService,
                             CourseService courseService,
                             TodoService todoService,
                             ExamService examService,
                             PeriodConfigService periodConfigService) {
        this.scheduleService = scheduleService;
        this.courseService = courseService;
        this.todoService = todoService;
        this.examService = examService;
        this.periodConfigService = periodConfigService;
    }

    @Override
    public Map<String, Object> overview(String apiKey, Long scheduleId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 1. 选定课表
        Schedule schedule;
        if (scheduleId != null) {
            schedule = scheduleService.getById(scheduleId, apiKey);
        } else {
            List<Schedule> all = scheduleService.list(apiKey);
            schedule = all.isEmpty() ? null : all.get(0);
        }
        Long sid = schedule == null ? null : schedule.getId();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", schedule == null ? "课程表" : schedule.getName());
        data.put("updatedAt", now.format(HM));
        data.put("week", weekInfo(schedule, today));
        data.put("today", WEEK_NAMES[today.getDayOfWeek().getValue() - 1] + " " + today.format(MD));

        // 2. 作息时间表 → 节次时间映射
        Map<Integer, PeriodConfig> periods = periodConfigService.list().stream()
                .collect(Collectors.toMap(PeriodConfig::getPeriodNumber, p -> p));

        // 3. 未来课程（今天剩余 + 该课表以后所有课，按时间排序，前端按可视高度裁切填满）
        List<Map<String, Object>> todayCourses = new ArrayList<>();
        Map<String, Object> nextClass = null;
        int todayRemaining = 0;
        if (schedule != null) {
            LocalDate firstMonday = schedule.getPeriodStartDate() == null
                    ? null
                    : schedule.getPeriodStartDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            List<Course> courses = courseService.listByScheduleId(sid, apiKey);

            for (Course c : courses) {
                if (c.getDayOfWeek() == null || c.getWeeks() == null || firstMonday == null) continue;
                int dow = c.getDayOfWeek();
                for (Integer wk : c.getWeeks()) {
                    // 第 wk 周、星期 dow 对应的日期
                    LocalDate day = firstMonday.plusDays((long) (wk - 1) * 7 + (dow - 1));
                    if (day.isBefore(today)) continue;
                    if (schedule.getPeriodEndDate() != null && day.isAfter(schedule.getPeriodEndDate())) continue;

                    PeriodConfig p0 = periods.get(c.getStartPeriod());
                    PeriodConfig p1 = periods.get(c.getEndPeriod());
                    LocalTime start = p0 == null ? null : p0.getStartTime();
                    LocalTime end = p1 == null ? null : p1.getEndTime();

                    // 今天的课若已结束则跳过
                    if (day.equals(today) && end != null && !end.isAfter(now.toLocalTime())) {
                        continue;
                    }

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", c.getName());
                    item.put("location", c.getLocation());
                    item.put("teacher", c.getTeacher());
                    item.put("period", c.getStartPeriod() + "-" + c.getEndPeriod() + "节");
                    item.put("time", (start != null ? start.format(HM) : "?") + "-" + (end != null ? end.format(HM) : "?"));
                    item.put("status", dayLabel(day, today, now.toLocalTime(), start, end));
                    item.put("date", day.format(MD));
                    item.put("week", wk);
                    item.put("dow", weekDayName(dow));
                    item.put("sortKey", toEpochMinute(day, start));
                    item.put("_day", day);
                    todayCourses.add(item);

                    if (day.equals(today)) todayRemaining++;

                    if (nextClass == null && end != null && end.isAfter(now.toLocalTime())
                            && start != null && start.isAfter(now.toLocalTime())) {
                        nextClass = item;
                    }
                }
            }
            todayCourses.sort((a, b) -> Long.compare(
                    (long) a.get("sortKey"), (long) b.get("sortKey")));
            // 移除仅用于排序的临时字段
            todayCourses.forEach(m -> m.remove("_day"));
        }
        data.put("todayCourses", todayCourses);
        data.put("todayRemaining", todayRemaining);
        data.put("nextClass", nextClass);

        // 4. 待办
        List<Map<String, Object>> todos = new ArrayList<>();
        int overdue = 0;
        List<Todo> all = todoService.list(apiKey);
        List<Todo> unfinished = all.stream()
                .filter(t -> !Boolean.TRUE.equals(t.getCompleted()))
                .sorted((a, b) -> a.getDdl() == null ? 1 : (b.getDdl() == null ? -1 : a.getDdl().compareTo(b.getDdl())))
                .collect(Collectors.toList());
        for (Todo t : unfinished) {
            boolean od = t.getDdl() != null && t.getDdl().isBefore(now);
            if (od) overdue++;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", t.getTitle());
            item.put("ddlText", t.getDdl() == null ? "无期限" : ddlText(t.getDdl(), today));
            item.put("ddlRemain", t.getDdl() == null ? "无期限" : remainText(t.getDdl(), now));
            item.put("overdue", od);
            todos.add(item);
        }
        data.put("todos", todos);
        data.put("overdueCount", overdue);
        data.put("todoSummary", "未完成 " + unfinished.size() + " 项");

        // 5. 即将到来的考试（已结束的不展示）
        List<Map<String, Object>> exams = new ArrayList<>();
        if (sid != null) {
            String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
            examService.listByScheduleId(sid, apiKey).stream()
                    .filter(e -> !e.getExamDate().isBefore(today)
                            && (e.getExamDate().isAfter(today) || e.getEndTime().isAfter(now.toLocalTime())))
                    .limit(3)
                    .forEach(e -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", e.getName());
                        item.put("location", e.getLocation());
                        item.put("time", e.getExamDate().format(MD) + " " + e.getStartTime().format(HM));
                        item.put("status", dayLabel(e.getExamDate(), today, now.toLocalTime(), e.getStartTime(), e.getEndTime()));
                        item.put("date", e.getExamDate().format(MD));
                        int examWeek = schedule == null ? 0 : weekOfDate(schedule, e.getExamDate());
                        item.put("week", examWeek);
                        item.put("dow", weekDays[e.getExamDate().getDayOfWeek().getValue() - 1]);
                        item.put("weekday", e.getExamDate().getDayOfWeek().getValue());
                        item.put("sortKey", toEpochMinute(e.getExamDate(), e.getStartTime()));
                        exams.add(item);
                    });
        }
        data.put("exams", exams);

        data.put("empty", todayCourses.isEmpty() && todos.isEmpty() && exams.isEmpty());
        return data;
    }

    private String weekInfo(Schedule schedule, LocalDate today) {
        if (schedule == null || schedule.getPeriodStartDate() == null) {
            return "学期未开始";
        }
        LocalDate start = schedule.getPeriodStartDate();
        LocalDate firstMonday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long diff = ChronoUnit.DAYS.between(firstMonday, today);
        int week = (int) (diff / 7) + 1;
        if (diff < 0) return "学期未开始";
        if (schedule.getPeriodEndDate() != null && today.isAfter(schedule.getPeriodEndDate())) return "学期已结束";
        return "第" + week + "周";
    }

    private int weekOfDate(Schedule schedule, LocalDate date) {
        if (schedule == null || schedule.getPeriodStartDate() == null) return 0;
        LocalDate firstMonday = schedule.getPeriodStartDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long diff = ChronoUnit.DAYS.between(firstMonday, date);
        return diff < 0 ? 0 : (int) (diff / 7) + 1;
    }

    private String weekDayName(int dow) {
        return WEEK_NAMES[dow - 1];
    }

    /** 统一时间排序键（分钟），课程与考试权重一致，可混排 */
    private long toEpochMinute(LocalDate date, LocalTime time) {
        return date.toEpochDay() * 1440 + (time == null ? 0 : time.toSecondOfDay() / 60);
    }

    private String courseStatus(LocalTime now, LocalTime start, LocalTime end) {
        if (start == null || end == null) return "—";
        if (now.isBefore(start)) {
            long minutes = ChronoUnit.MINUTES.between(now, start);
            return "还有" + Math.max(1, (minutes + 59) / 60) + "小时";
        }
        if (now.isBefore(end)) return "进行中";
        return "已结束";
    }

    /** 课程状态：今天/明天/后天/更远 */
    private String dayLabel(LocalDate day, LocalDate today, LocalTime now, LocalTime start, LocalTime end) {
        if (day.equals(today)) return courseStatus(now, start, end);
        if (day.equals(today.plusDays(1))) return "明天";
        if (day.equals(today.plusDays(2))) return "后天";
        return "非最近";
    }

    private String ddlText(LocalDateTime ddl, LocalDate today) {
        LocalDate d = ddl.toLocalDate();
        String prefix;
        if (d.equals(today)) prefix = "今天";
        else if (d.equals(today.plusDays(1))) prefix = "明天";
        else prefix = d.format(MD);
        return prefix + " " + ddl.format(HM);
    }

    /** 距 DDL 剩余时长，按小时精度展示即可 */
    private String remainText(LocalDateTime ddl, LocalDateTime now) {
        long sec = ChronoUnit.SECONDS.between(now, ddl);
        if (sec < 0) return "已过期";
        long totalHours = (sec + 3599) / 3600; // 向上取整到小时
        if (totalHours < 1) return "不足 1 小时";
        long days = totalHours / 24;
        long hours = totalHours % 24;
        if (days > 0) return days + "天" + hours + "小时";
        return hours + "小时";
    }
}