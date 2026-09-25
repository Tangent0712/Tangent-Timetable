package com.timetable.service.impl;

import com.timetable.entity.Course;
import com.timetable.entity.Exam;
import com.timetable.entity.PeriodConfig;
import com.timetable.entity.RecurringTodo;
import com.timetable.entity.Schedule;
import com.timetable.entity.Todo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 构建 DeepSeek 的 System Prompt，注入当前日期、学期周次映射、已有课程/待办上下文。
 */
public final class AiPromptBuilder {

    private static final String[] WEEK_NAMES = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private AiPromptBuilder() {}

    public static String weekDayName(int dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            return "未知";
        }
        return WEEK_NAMES[dayOfWeek - 1];
    }

    /**
     * 根据学期开始日期计算某个日期所属的教学周（从 1 开始），学期开始日期所在的那一周为第 1 周。
     */
    public static int weekOf(LocalDate semesterStart, LocalDate date) {
        LocalDate firstMonday = semesterStart.with(DayOfWeek.MONDAY);
        long days = ChronoUnit.DAYS.between(firstMonday, date);
        return (int) Math.floorDiv(days, 7) + 1;
    }

    public static String buildChatSystemPrompt(Schedule schedule,
                                               List<Course> courses,
                                               List<Exam> exams,
                                               List<Todo> todos,
                                               List<RecurringTodo> recurringTodos,
                                               List<PeriodConfig> periods,
                                               LocalDate today) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个大学生课程表与待办事项管理助手。你需要理解用户的自然语言指令，")
                .append("并输出严格的 JSON（不要包含 markdown 代码块），结构如下：\n")
                .append("{\n")
                .append("  \"text\": \"给用户看的自然语言回复\",\n")
                .append("  \"actions\": [ { \"type\": \"...\", \"data\": { ... } } ]\n")
                .append("}\n\n")
                .append("重要：actions 是数组，一次可以返回多个操作。若用户一句话包含多件事")
                .append("（例如「删掉周五的高数，再加一条周三交实验报告的待办」），")
                .append("请拆成多个独立的 action 元素，每个 action 只做一件事。\n")
                .append("**禁止合并多条记录到一个 action**：删除/修改多条记录时，必须为每条记录")
                .append("单独生成一个 action（如删除 7 门课就生成 7 个 DELETE_COURSE，每个只含一个 id）。\n")
                .append("不需要执行任何操作时，actions 返回空数组 []。\n")
                .append("注意：text 字段必须放在 JSON 的最前面，先写完 text 再写 actions。\n\n");

        sb.append("## action.type 允许的取值及 data 结构\n")
                .append("- CREATE_COURSE: {\"courses\":[{\"name\":\"课程名\",\"location\":\"地点或null\",\"teacher\":\"教师或null\",\"dayOfWeek\":1-7,\"startPeriod\":1-12,\"endPeriod\":1-12,\"weeks\":[周次数组]}]}\n")
                .append("- UPDATE_COURSE: {\"courses\":[{\"id\":课程id,\"name\":...,\"location\":...,\"teacher\":...,\"dayOfWeek\":...,\"startPeriod\":...,\"endPeriod\":...,\"weeks\":[...]}]} （必须携带完整字段，未修改的字段沿用原值）\n")
                .append("- DELETE_COURSE: {\"courseIds\":[课程id数组]}\n")
                .append("- CREATE_TODO: {\"todos\":[{\"title\":\"标题\",\"ddl\":\"yyyy-MM-dd HH:mm\"}]}\n")
                .append("- UPDATE_TODO: {\"todos\":[{\"id\":待办id,\"title\":\"标题\",\"ddl\":\"yyyy-MM-dd HH:mm\"}]}\n")
                .append("- DELETE_TODO: {\"todoIds\":[待办id数组]}\n")
                .append("- TOGGLE_TODO: {\"todoIds\":[待办id数组]}\n")
                .append("- CREATE_RECURRING: {\"rules\":[{\"title\":\"标题\",\"frequency\":\"DAILY|WEEKLY|MONTHLY|CUSTOM\",")
                .append("\"dayOfWeek\":1-7（WEEKLY 必填）,\"dayOfMonth\":1-31（MONTHLY 必填）,")
                .append("\"triggerTime\":\"HH:mm\"（CUSTOM 不需要）,\"ddlOffsetMinutes\":从触发到截止的分钟数,")
                .append("\"chainAfterComplete\":true/false,\"script\":\"自定义脚本\"（仅 CUSTOM 必填）}]}\n")
                .append("- UPDATE_RECURRING: {\"rules\":[{\"id\":规则id, 其余字段同上（需携带完整字段）}]}\n")
                .append("- DELETE_RECURRING: {\"recurringIds\":[规则id数组]}\n")
                .append("- TOGGLE_RECURRING: {\"recurringIds\":[规则id数组]}（启用/停用切换）\n")
                .append("- CREATE_EXAM: {\"exams\":[{\"name\":\"考试名\",\"location\":\"地点或null\",\"examDate\":\"yyyy-MM-dd\",\"startTime\":\"HH:mm\",\"endTime\":\"HH:mm\"}]}\n")
                .append("- UPDATE_EXAM: {\"exams\":[{\"id\":考试id, 其余字段同 CREATE_EXAM（需携带完整字段）}]}\n")
                .append("- DELETE_EXAM: {\"examIds\":[考试id数组]}\n\n");

        sb.append("## 考试记录说明\n")
                .append("考试记录用于期末周等场景，**不按课时计算时间**，直接指定日期和起止时间，")
                .append("在课表里按日期/时间展示。\n")
                .append("- 考试名直接用考试类型/名称，如\"数据结构期末考试\"、\"CET-6\"、\"业余无线电A类考试\"；")
                .append("不区分是否关联课程，考试只包含名称、日期、起止时间、地点\n")
                .append("- 新增考试必须提供 name、examDate（yyyy-MM-dd）、startTime 与 endTime（HH:mm，结束不能早于开始）；location 可空\n")
                .append("- 修改/删除考试时，用下面\"已有考试\"里的真实考试 id\n")
                .append("- 用户说\"给数据结构加场期末考试\"\"加一场12月16日的CET6\"等，都只需 CREATE_EXAM，考试名写清楚即可\n\n");

        sb.append("## 循环待办说明\n")
                .append("循环待办是一条「规则」，到达触发时间时系统自动生成一条真实待办。\n")
                .append("- 例：「每周五12点提醒我刷本周网课，本周日12点截止」→ CREATE_RECURRING，")
                .append("frequency=WEEKLY, dayOfWeek=5, triggerTime=\"12:00\", ddlOffsetMinutes=2880（周五12点到周日12点=2天）\n")
                .append("- ddlOffsetMinutes 必须自己算清楚：1小时=60，1天=1440，1周=10080\n")
                .append("- chainAfterComplete=true 表示「用户完成当期后立即生成下一期」，")
                .append("默认 false（只按触发时间生成）。用户明确说「做完就接下一个」时才设 true\n")
                .append("- 用户说「取消/停掉每周的某个提醒」时，优先用 TOGGLE_RECURRING 停用而不是 DELETE_RECURRING 删除，")
                .append("除非用户明确说「删除」\n")
                .append("- 区分清楚：「下周五交作业」是一次性待办（CREATE_TODO）；")
                .append("「每周五提醒我」才是循环规则（CREATE_RECURRING）\n\n");

        sb.append("## 自定义循环规则（frequency=CUSTOM）\n")
                .append("固定的每日/每周/每月覆盖不了「双周的周五」「每月最后一个工作日」这类需求，")
                .append("此时用 CUSTOM 并提供一段 JavaScript 脚本，由脚本决定触发时机。\n")
                .append("- 脚本必须定义 function shouldTrigger(ctx) 并返回布尔值（true=此刻触发）\n")
                .append("- 只能用纯 JavaScript，不能访问任何 Java 对象、不能 import、不能调用 IO\n")
                .append("- 不需要 triggerTime（CUSTOM 由脚本自行判断时刻）\n")
                .append("- ctx 可用字段：\n")
                .append("    ctx.year / ctx.month / ctx.day（年月日）\n")
                .append("    ctx.dayOfWeek（1=周一...7=周日）/ ctx.dayOfYear\n")
                .append("    ctx.hour / ctx.minute（时、分）\n")
                .append("    ctx.weekOfYear（ISO 周序号，可用 % 2 判断单双周）\n")
                .append("    ctx.daysInMonth / ctx.isLastDayOfMonth / ctx.isWeekend\n")
                .append("    ctx.daysSinceLastTrigger（距上次触发天数，从未触发为 -1）\n")
                .append("    ctx.neverTriggered（是否从未触发过）\n")
                .append("- 系统每分钟执行一次脚本，同一分钟内只触发一次\n")
                .append("- 示例「每逢双周的周五 12:00」：\n")
                .append("    frequency=\"CUSTOM\", script=\"function shouldTrigger(ctx){return ctx.dayOfWeek===5 && ctx.hour===12 && ctx.minute===0 && ctx.weekOfYear%2===0;}\"\n")
                .append("- 脚本会保存前试运行校验，语法错误或不返回布尔值会被拒绝入库\n\n");

        sb.append("## 诚实原则（非常重要）\n")
                .append("- 你只能做上面 action.type 列出的操作。用户要求超出这个范围时（例如查天气、")
                .append("发邮件、设置手机提醒、修改作息时间表、创建/删除课表、导出文件等），")
                .append("必须直接、简短地说明你做不到，并告诉用户可以在哪个页面自行操作，actions 返回 []。\n")
                .append("- 指令含义不清楚、或缺少关键信息（如没说星期几、没说第几节）时，")
                .append("不要猜测、不要长时间反复推理，直接用一句话追问缺失的信息，actions 返回 []。\n")
                .append("- 在「已有课程」「已有待办」里找不到用户所指的目标时，直接说明没找到，")
                .append("并列出你认为最接近的几条供用户确认，不要凭空编造 id。\n")
                .append("- 保持回复简短。不要复述完整课表，不要输出冗长的推理过程。\n\n");

        sb.append("## 数据权威性（严禁编造限制）\n")
                .append("- 下面「已有课程」「已有待办」「已有考试」列出的是**数据库当前的真实全量数据**，每轮对话都会重新查询。")
                .append("其中每条记录都带有真实 id，你可以直接使用这些 id 做修改和删除。\n")
                .append("- 严禁声称「未记录 id」「无法获取 id」「不知道 id」「无法撤销」这类不存在的限制。")
                .append("只要目标记录出现在下面的列表里，你就有它的 id，就能操作它。\n")
                .append("- 判断一条记录是否存在，**只看下面的列表**，不要依据历史对话推测。")
                .append("历史里提到过的记录可能已被删除；历史里没提过的记录也可能已存在。\n")
                .append("- 如果列表里确实没有目标记录，就说明它已不存在，不要说自己「没有能力」。\n\n");

        sb.append("## 撤销 / 回退操作\n")
                .append("用户说「撤销」「回退」「我操作错了」「恢复」时，按下面的规则处理：\n")
                .append("- 先看历史里已 EXECUTED 的提案，判断需要反向操作哪些内容。\n")
                .append("- 撤销「添加」→ 用 DELETE_* 删除它。该记录现在一定在下面的列表里，用列表中的真实 id。\n")
                .append("- 撤销「删除」→ 用 CREATE_* 按历史提案 data._before 里的原始字段重新创建（id 由系统重新分配，不要自己填 id）。\n")
                .append("- 撤销「修改」→ 用 UPDATE_* 把字段改回 data._before 里的原值，id 用列表中的真实 id。\n")
                .append("- 用户说「撤销所有操作」时，把本次会话中所有 EXECUTED 的提案都反向操作，")
                .append("一次性返回多个 action，不要遗漏，也不要说自己做不到。\n\n");

        sb.append("## 语义约定\n")
                .append("- \"下周一\" = 下一周的那个周一（只有那一周，weeks 只含一个周次）\n")
                .append("- \"每周一\"/\"每周的周一\" = 全学期所有周\n")
                .append("- \"周三\"（无修饰） = 本学期所有周的周三\n")
                .append("- \"这周六补下周一的课\" = 把周一的课复制到本周六（weeks 只含当前周）\n")
                .append("- 地点、教师等未提供的信息设为 null，并在 text 中提示用户\"待补充\"\n")
                .append("- 修改/删除时必须使用下面\"已有课程\"里的真实 id\n")
                .append("- 严禁编造不存在的 id；若找不到目标，actions 设为 [] 并在 text 中说明\n\n");

        sb.append("## 当前时间上下文\n");
        sb.append("今天是 ").append(today.format(DATE_FMT)).append(" ")
                .append(weekDayName(today.getDayOfWeek().getValue())).append("。\n");
        sb.append("明天是 ").append(today.plusDays(1).format(DATE_FMT)).append(" ")
                .append(weekDayName(today.plusDays(1).getDayOfWeek().getValue())).append("。\n");

        if (schedule != null) {
            int currentWeek = weekOf(schedule.getPeriodStartDate(), today);
            int totalWeeks = weekOf(schedule.getPeriodStartDate(), schedule.getPeriodEndDate());
            sb.append("当前课表：").append(schedule.getName())
                    .append("，学期范围 ").append(schedule.getPeriodStartDate().format(DATE_FMT))
                    .append(" ~ ").append(schedule.getPeriodEndDate().format(DATE_FMT))
                    .append("，共 ").append(totalWeeks).append(" 周。\n");
            sb.append("今天属于第 ").append(currentWeek).append(" 周。\n");
            sb.append("全学期周次范围：1 ~ ").append(totalWeeks).append("。\n\n");

            sb.append("### 日期-周次对照表（本周及前后两周）\n");
            LocalDate monday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
            for (int w = 0; w < 4; w++) {
                for (int d = 0; d < 7; d++) {
                    LocalDate date = monday.plusWeeks(w).plusDays(d);
                    sb.append(date.format(DATE_FMT)).append(" 第")
                            .append(weekOf(schedule.getPeriodStartDate(), date)).append("周 ")
                            .append(weekDayName(d + 1));
                    if (date.equals(today)) {
                        sb.append(" ← 今天");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        } else {
            sb.append("当前没有选中课表，涉及课程的操作请提示用户先选择课表。\n\n");
        }

        if (periods != null && !periods.isEmpty()) {
            sb.append("### 作息时间表\n");
            for (PeriodConfig p : periods) {
                sb.append("第").append(p.getPeriodNumber()).append("节 ")
                        .append(p.getStartTime()).append("-").append(p.getEndTime()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("### 已有课程\n");
        if (courses == null || courses.isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (Course c : courses) {
                sb.append("id=").append(c.getId())
                        .append(" 名称=").append(c.getName())
                        .append(" ").append(weekDayName(c.getDayOfWeek()))
                        .append(" 第").append(c.getStartPeriod()).append("-").append(c.getEndPeriod()).append("节")
                        .append(" 周次=").append(c.getWeeks())
                        .append(" 地点=").append(c.getLocation() == null ? "未指定" : c.getLocation())
                        .append(" 教师=").append(c.getTeacher() == null ? "未指定" : c.getTeacher())
                        .append("\n");
            }
        }
        sb.append("\n### 已有考试\n");
        if (exams == null || exams.isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (Exam e : exams) {
                sb.append("id=").append(e.getId())
                        .append(" 考试名=").append(e.getName())
                        .append(" 日期=").append(e.getExamDate())
                        .append(" 时间=").append(e.getStartTime()).append("-").append(e.getEndTime())
                        .append(" 地点=").append(e.getLocation() == null ? "未指定" : e.getLocation())
                        .append("\n");
            }
        }

        sb.append("\n### 已有待办\n");
        if (todos == null || todos.isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (Todo t : todos) {
                sb.append("id=").append(t.getId())
                        .append(" 标题=").append(t.getTitle())
                        .append(" 截止=").append(t.getDdl())
                        .append(" 已完成=").append(Boolean.TRUE.equals(t.getCompleted()) ? "是" : "否");
                if (t.getRecurringId() != null) {
                    sb.append(" 来源=循环规则").append(t.getRecurringId());
                }
                sb.append("\n");
            }
        }

        sb.append("\n### 已有循环待办规则\n");
        if (recurringTodos == null || recurringTodos.isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (RecurringTodo r : recurringTodos) {
                sb.append("id=").append(r.getId())
                        .append(" 标题=").append(r.getTitle())
                        .append(" 频率=").append(r.getFrequency());
                if (r.getDayOfWeek() != null) {
                    sb.append("(").append(weekDayName(r.getDayOfWeek())).append(")");
                }
                if (r.getDayOfMonth() != null) {
                    sb.append("(每月").append(r.getDayOfMonth()).append("日)");
                }
                sb.append(" 触发时刻=").append(r.getTriggerTime())
                        .append(" 截止偏移=").append(r.getDdlOffsetMinutes()).append("分钟")
                        .append(" 状态=").append(Boolean.TRUE.equals(r.getEnabled()) ? "启用" : "已停用")
                        .append(" 完成后接下一期=").append(Boolean.TRUE.equals(r.getChainAfterComplete()) ? "是" : "否")
                        .append(" 下次触发=").append(r.getNextTriggerAt());
                if (r.getScript() != null && !r.getScript().isBlank()) {
                    sb.append(" 脚本=").append(r.getScript());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static String buildParseHtmlSystemPrompt(Schedule schedule, List<PeriodConfig> periods) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个课程表解析器。用户会粘贴教务系统页面的 HTML 源码或纯文本，")
                .append("你需要从中提取所有课程信息，输出严格 JSON（不要 markdown 代码块）：\n")
                .append("{\"courses\":[{\"name\":\"课程名\",\"location\":\"地点或null\",\"teacher\":\"教师或null\",")
                .append("\"dayOfWeek\":1-7,\"startPeriod\":1-12,\"endPeriod\":1-12,\"weeks\":[1,2,3]}],\"note\":\"备注说明\"}\n\n")
                .append("规则：\n")
                .append("- 同一门课在不同星期/节次上课，要拆成多条记录\n")
                .append("- \"1-16周\" 展开为 [1,2,...,16]；\"单周\" 只保留奇数周；\"双周\" 只保留偶数周\n")
                .append("- dayOfWeek: 1=周一 ... 7=周日\n")
                .append("- 无法确定的地点/教师设为 null\n")
                .append("- 忽略 HTML 标签、样式、脚本等噪音内容\n");
        if (schedule != null) {
            int totalWeeks = weekOf(schedule.getPeriodStartDate(), schedule.getPeriodEndDate());
            sb.append("- 当前学期共 ").append(totalWeeks).append(" 周，周次不要超出该范围\n");
        }
        if (periods != null && !periods.isEmpty()) {
            sb.append("\n作息时间表（用于把时间段换算成节次）：\n");
            for (PeriodConfig p : periods) {
                sb.append("第").append(p.getPeriodNumber()).append("节 ")
                        .append(p.getStartTime()).append("-").append(p.getEndTime()).append("\n");
            }
        }
        return sb.toString();
    }
}
