package com.timetable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.timetable.dto.RecurringTodoRequest;
import com.timetable.entity.RecurringTodo;
import com.timetable.entity.Todo;
import com.timetable.exception.BusinessException;
import com.timetable.mapper.RecurringTodoMapper;
import com.timetable.mapper.TodoMapper;
import com.timetable.service.RecurringTodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RecurringTodoServiceImpl implements RecurringTodoService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTodoServiceImpl.class);

    /** 停机回补上限：单条规则一次最多补生成的期数，避免长期停机后刷出大量待办 */
    private static final int MAX_CATCH_UP = 5;

    private final RecurringTodoMapper recurringMapper;
    private final TodoMapper todoMapper;
    private final RecurringScriptEvaluator scriptEvaluator;

    public RecurringTodoServiceImpl(RecurringTodoMapper recurringMapper, TodoMapper todoMapper,
                                    RecurringScriptEvaluator scriptEvaluator) {
        this.recurringMapper = recurringMapper;
        this.todoMapper = todoMapper;
        this.scriptEvaluator = scriptEvaluator;
    }

    @Override
    public List<RecurringTodo> list(String apiKey) {
        LambdaQueryWrapper<RecurringTodo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecurringTodo::getApiKey, apiKey).orderByAsc(RecurringTodo::getNextTriggerAt);
        return recurringMapper.selectList(wrapper);
    }

    @Override
    public RecurringTodo getById(Long id, String apiKey) {
        RecurringTodo rule = recurringMapper.selectById(id);
        if (rule == null || !rule.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "循环任务不存在");
        }
        return rule;
    }

    private void apply(RecurringTodo rule, RecurringTodoRequest request) {
        rule.setTitle(request.getTitle().trim());
        rule.setFrequency(request.getFrequency().trim().toUpperCase());
        rule.setDayOfWeek(request.getDayOfWeek());
        rule.setDayOfMonth(request.getDayOfMonth());
        rule.setTriggerTime(request.getTriggerTime());
        rule.setScript(request.getScript());
        rule.setDdlOffsetMinutes(request.getDdlOffsetMinutes());
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getChainAfterComplete() != null) {
            rule.setChainAfterComplete(request.getChainAfterComplete());
        }
        // 频率相关字段互斥，避免残留脏数据
        if (!RecurringScheduleCalculator.WEEKLY.equals(rule.getFrequency())) {
            rule.setDayOfWeek(null);
        }
        if (!RecurringScheduleCalculator.MONTHLY.equals(rule.getFrequency())) {
            rule.setDayOfMonth(null);
        }
        if (RecurringScheduleCalculator.CUSTOM.equals(rule.getFrequency())) {
            rule.setTriggerTime(null);
            rule.setDayOfWeek(null);
            rule.setDayOfMonth(null);
        } else {
            rule.setScript(null);
        }
        RecurringScheduleCalculator.validate(rule);

        // 自定义脚本必须先试运行通过才允许保存，避免坏脚本进入调度循环
        if (RecurringScheduleCalculator.CUSTOM.equals(rule.getFrequency())) {
            RecurringScriptEvaluator.ScriptResult probe =
                    scriptEvaluator.test(rule.getScript(), LocalDateTime.now(), null);
            if (!probe.ok()) {
                throw new BusinessException(400, "脚本校验失败：" + probe.error());
            }
        }
    }

    @Override
    public RecurringTodo create(RecurringTodoRequest request, String apiKey) {
        RecurringTodo rule = new RecurringTodo();
        rule.setApiKey(apiKey);
        rule.setEnabled(true);
        rule.setChainAfterComplete(false);
        apply(rule, request);
        rule.setNextTriggerAt(
                RecurringScheduleCalculator.nextTriggerAfter(rule, LocalDateTime.now()));
        recurringMapper.insert(rule);
        return rule;
    }

    @Override
    public RecurringTodo update(Long id, RecurringTodoRequest request, String apiKey) {
        RecurringTodo rule = getById(id, apiKey);
        apply(rule, request);
        // 规则变了要重算下次触发时间
        rule.setNextTriggerAt(
                RecurringScheduleCalculator.nextTriggerAfter(rule, LocalDateTime.now()));
        recurringMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional
    public void delete(Long id, String apiKey) {
        getById(id, apiKey);
        // 已生成的待办保留，仅解除关联，避免用户丢失历史记录
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getRecurringId, id);
        for (Todo todo : todoMapper.selectList(wrapper)) {
            todo.setRecurringId(null);
            // updateById 会忽略 null，这里用 UpdateWrapper 显式置空
            todoMapper.update(null,
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<Todo>lambdaUpdate()
                            .eq(Todo::getId, todo.getId())
                            .set(Todo::getRecurringId, null));
        }
        recurringMapper.deleteById(id);
    }

    @Override
    public RecurringTodo toggleEnabled(Long id, String apiKey) {
        RecurringTodo rule = getById(id, apiKey);
        boolean next = !Boolean.TRUE.equals(rule.getEnabled());
        rule.setEnabled(next);
        if (next) {
            // 重新启用时从当前时间往后排，不补历史
            rule.setNextTriggerAt(
                    RecurringScheduleCalculator.nextTriggerAfter(rule, LocalDateTime.now()));
        }
        recurringMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional
    public RecurringTodo triggerNow(Long id, String apiKey) {
        RecurringTodo rule = getById(id, apiKey);
        LocalDateTime now = LocalDateTime.now();
        spawnTodo(rule, now);
        rule.setLastTriggeredAt(now);
        // CUSTOM 无固定下次触发时间（返回 null），由脚本每分钟判断
        rule.setNextTriggerAt(RecurringScheduleCalculator.nextTriggerAfter(rule, now));
        recurringMapper.updateById(rule);
        return rule;
    }

    @Override
    public RecurringScriptEvaluator.ScriptResult testScript(String script) {
        return scriptEvaluator.test(script, LocalDateTime.now(), null);
    }

    /** 按规则生成一条待办，ddl = 触发时刻 + 偏移 */
    private void spawnTodo(RecurringTodo rule, LocalDateTime triggeredAt) {
        Todo todo = new Todo();
        todo.setApiKey(rule.getApiKey());
        todo.setTitle(rule.getTitle());
        todo.setDdl(RecurringScheduleCalculator.ddlOf(rule, triggeredAt));
        todo.setCompleted(false);
        todo.setRecurringId(rule.getId());
        todoMapper.insert(todo);
    }

    @Override
    @Transactional
    public int generateDueTodos() {
        LocalDateTime now = LocalDateTime.now();
        int generated = generateForFixedRules(now) + generateForCustomRules(now);
        if (generated > 0) {
            log.info("Recurring todos generated: {}", generated);
        }
        return generated;
    }

    /**
     * 自定义脚本规则：每次扫描都执行一次 shouldTrigger(ctx)。
     * 用「同一分钟内不重复触发」做去重，因为调度器每 60s 跑一次，
     * 存在同一分钟被扫到两次的可能。
     */
    private int generateForCustomRules(LocalDateTime now) {
        LambdaQueryWrapper<RecurringTodo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecurringTodo::getEnabled, true)
                .eq(RecurringTodo::getFrequency, RecurringScheduleCalculator.CUSTOM);

        int generated = 0;
        for (RecurringTodo rule : recurringMapper.selectList(wrapper)) {
            LocalDateTime minute = now.truncatedTo(ChronoUnit.MINUTES);
            if (rule.getLastTriggeredAt() != null
                    && !rule.getLastTriggeredAt().truncatedTo(ChronoUnit.MINUTES).isBefore(minute)) {
                continue;
            }
            RecurringScriptEvaluator.ScriptResult r =
                    scriptEvaluator.test(rule.getScript(), now, rule.getLastTriggeredAt());
            if (!r.ok()) {
                // 脚本出错则自动停用，避免每分钟反复报错
                log.warn("Custom recurring rule {} disabled due to script error: {}",
                        rule.getId(), r.error());
                rule.setEnabled(false);
                recurringMapper.updateById(rule);
                continue;
            }
            if (r.triggered()) {
                spawnTodo(rule, minute);
                rule.setLastTriggeredAt(minute);
                recurringMapper.updateById(rule);
                generated++;
            }
        }
        return generated;
    }

    private int generateForFixedRules(LocalDateTime now) {
        LambdaQueryWrapper<RecurringTodo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecurringTodo::getEnabled, true)
                .ne(RecurringTodo::getFrequency, RecurringScheduleCalculator.CUSTOM)
                .isNotNull(RecurringTodo::getNextTriggerAt)
                .le(RecurringTodo::getNextTriggerAt, now);

        int generated = 0;
        for (RecurringTodo rule : recurringMapper.selectList(wrapper)) {
            LocalDateTime due = rule.getNextTriggerAt();
            int rounds = 0;
            // 补齐停机期间错过的触发点，但设上限防止刷屏
            while (due != null && !due.isAfter(now) && rounds < MAX_CATCH_UP) {
                spawnTodo(rule, due);
                rule.setLastTriggeredAt(due);
                due = RecurringScheduleCalculator.nextTriggerAfter(rule, due);
                rounds++;
                generated++;
            }
            // 若仍落后于当前时间（错过太多期），直接跳到下一个未来触发点
            if (due != null && !due.isAfter(now)) {
                due = RecurringScheduleCalculator.nextTriggerAfter(rule, now);
            }
            rule.setNextTriggerAt(due);
            recurringMapper.updateById(rule);
        }
        return generated;
    }

    @Override
    @Transactional
    public void onTodoCompleted(Long recurringId, String apiKey) {
        RecurringTodo rule = recurringMapper.selectById(recurringId);
        if (rule == null || !rule.getApiKey().equals(apiKey)) {
            return;
        }
        if (!Boolean.TRUE.equals(rule.getEnabled())
                || !Boolean.TRUE.equals(rule.getChainAfterComplete())) {
            return;
        }
        // 完成即接下一期：以「下次触发时间」为基准生成，保持周期节奏而非从当前时刻起算
        LocalDateTime base = rule.getNextTriggerAt() != null
                ? rule.getNextTriggerAt() : LocalDateTime.now();
        spawnTodo(rule, base);
        rule.setLastTriggeredAt(LocalDateTime.now());
        rule.setNextTriggerAt(RecurringScheduleCalculator.nextTriggerAfter(rule, base));
        recurringMapper.updateById(rule);
    }
}
