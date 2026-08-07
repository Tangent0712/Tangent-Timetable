package com.timetable.service.impl;

import com.timetable.entity.Course;
import com.timetable.entity.PeriodConfig;
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
                                               List<Todo> todos,
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
                .append("不需要执行任何操作时，actions 返回空数组 []。\n")
                .append("注意：text 字段必须放在 JSON 的最前面，先写完 text 再写 actions。\n\n");

        sb.append("## action.type 允许的取值及 data 结构\n")
                .append("- CREATE_COURSE: {\"courses\":[{\"name\":\"课程名\",\"location\":\"地点或null\",\"teacher\":\"教师或null\",\"dayOfWeek\":1-7,\"startPeriod\":1-12,\"endPeriod\":1-12,\"weeks\":[周次数组]}]}\n")
                .append("- UPDATE_COURSE: {\"courses\":[{\"id\":课程id,\"name\":...,\"location\":...,\"teacher\":...,\"dayOfWeek\":...,\"startPeriod\":...,\"endPeriod\":...,\"weeks\":[...]}]} （必须携带完整字段，未修改的字段沿用原值）\n")
                .append("- DELETE_COURSE: {\"courseIds\":[课程id数组]}\n")
                .append("- CREATE_TODO: {\"todos\":[{\"title\":\"标题\",\"ddl\":\"yyyy-MM-dd HH:mm\"}]}\n")
                .append("- UPDATE_TODO: {\"todos\":[{\"id\":待办id,\"title\":\"标题\",\"ddl\":\"yyyy-MM-dd HH:mm\"}]}\n")
                .append("- DELETE_TODO: {\"todoIds\":[待办id数组]}\n")
                .append("- TOGGLE_TODO: {\"todoIds\":[待办id数组]}\n\n");

        sb.append("## 诚实原则（非常重要）\n")
                .append("- 你只能做上面列出的 7 种操作。用户要求超出这个范围时（例如查天气、")
                .append("发邮件、设置手机提醒、修改作息时间表、创建/删除课表、导出文件等），")
                .append("必须直接、简短地说明你做不到，并告诉用户可以在哪个页面自行操作，actions 返回 []。\n")
                .append("- 指令含义不清楚、或缺少关键信息（如没说星期几、没说第几节）时，")
                .append("不要猜测、不要长时间反复推理，直接用一句话追问缺失的信息，actions 返回 []。\n")
                .append("- 在「已有课程」「已有待办」里找不到用户所指的目标时，直接说明没找到，")
                .append("并列出你认为最接近的几条供用户确认，不要凭空编造 id。\n")
                .append("- 保持回复简短。不要复述完整课表，不要输出冗长的推理过程。\n\n");

        sb.append("## 数据权威性（严禁编造限制）\n")
                .append("- 下面「已有课程」「已有待办」列出的是**数据库当前的真实全量数据**，每轮对话都会重新查询。")
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
        sb.append("\n### 已有待办\n");
        if (todos == null || todos.isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (Todo t : todos) {
                sb.append("id=").append(t.getId())
                        .append(" 标题=").append(t.getTitle())
                        .append(" 截止=").append(t.getDdl())
                        .append(" 已完成=").append(Boolean.TRUE.equals(t.getCompleted()) ? "是" : "否")
                        .append("\n");
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
