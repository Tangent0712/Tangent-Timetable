package com.timetable.service;

import java.util.Map;

/**
 * 小组件 / WebView 展示数据聚合。面向只读展示页（iPad 小组件、Android 全屏应用），
 * 数据随每次请求实时计算（展示页每 60 秒轮询一次）。
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