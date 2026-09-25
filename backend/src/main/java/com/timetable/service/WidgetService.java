package com.timetable.service;

import java.util.Map;

/**
 * 桌面小组件展示数据聚合。面向宽4×高2 的只读小组件页，
 * 数据随每次请求实时计算（小组件每 15 分钟轮询一次）。
 * 鉴权通过 URL 中的 key 参数完成，不走 Session/Cookie。
 */
public interface WidgetService {

    /**
     * 聚合课表 + 待办 + 考试，返回扁平、紧凑的展示结构。
     *
     * @param apiKey     当前用户 key（已由 WidgetController 校验）
     * @param scheduleId 可选；为空时自动取该用户最近创建的课表
     */
    Map<String, Object> overview(String apiKey, Long scheduleId);
}