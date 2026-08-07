package com.timetable.service;

import com.timetable.dto.RecurringTodoRequest;
import com.timetable.entity.RecurringTodo;

import java.util.List;

public interface RecurringTodoService {

    List<RecurringTodo> list(String apiKey);

    RecurringTodo getById(Long id, String apiKey);

    RecurringTodo create(RecurringTodoRequest request, String apiKey);

    RecurringTodo update(Long id, RecurringTodoRequest request, String apiKey);

    void delete(Long id, String apiKey);

    /** 启用/停用循环规则 */
    RecurringTodo toggleEnabled(Long id, String apiKey);

    /** 立即触发一次，生成当期待办（用于手动补生成与测试） */
    RecurringTodo triggerNow(Long id, String apiKey);

    /** 试运行自定义脚本，返回是否合法与当前是否会触发 */
    com.timetable.service.impl.RecurringScriptEvaluator.ScriptResult testScript(String script);

    /**
     * 扫描所有到期规则并生成待办，返回生成条数。
     * 会补齐停机期间错过的触发（同一规则每周期最多补一条）。
     */
    int generateDueTodos();

    /**
     * 某条由循环规则生成的待办被标记完成后调用。
     * 若规则开启了 chainAfterComplete，则立即生成下一期。
     */
    void onTodoCompleted(Long recurringId, String apiKey);
}
