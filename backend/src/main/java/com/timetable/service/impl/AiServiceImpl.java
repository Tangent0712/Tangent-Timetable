package com.timetable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.timetable.dto.AiActionDto;
import com.timetable.dto.AiConversationRequest;
import com.timetable.dto.AiMessageRequest;
import com.timetable.dto.AiMessageResponse;
import com.timetable.dto.CourseRequest;
import com.timetable.dto.RecurringTodoRequest;
import com.timetable.dto.TodoRequest;
import com.timetable.entity.AiAction;
import com.timetable.entity.AiConversation;
import com.timetable.entity.AiMessage;
import com.timetable.entity.Course;
import com.timetable.entity.PeriodConfig;
import com.timetable.entity.RecurringTodo;
import com.timetable.entity.Schedule;
import com.timetable.entity.Todo;
import com.timetable.exception.BusinessException;
import com.timetable.mapper.AiActionMapper;
import com.timetable.mapper.AiConversationMapper;
import com.timetable.mapper.AiMessageMapper;
import com.timetable.service.AiService;
import com.timetable.service.CourseService;
import com.timetable.service.DeepSeekClient;
import com.timetable.service.PeriodConfigService;
import com.timetable.service.RecurringTodoService;
import com.timetable.service.ScheduleService;
import com.timetable.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final int HISTORY_LIMIT = 20;
    private static final int HTML_MAX_LENGTH = 60000;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_EXECUTED = "EXECUTED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_STALE = "STALE";

    public static final String SCOPE_COURSE = "COURSE";
    public static final String SCOPE_TODO = "TODO";

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiActionMapper actionMapper;
    private final DeepSeekClient deepSeekClient;
    private final ScheduleService scheduleService;
    private final CourseService courseService;
    private final TodoService todoService;
    private final RecurringTodoService recurringTodoService;
    private final PeriodConfigService periodConfigService;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(AiConversationMapper conversationMapper,
                         AiMessageMapper messageMapper,
                         AiActionMapper actionMapper,
                         DeepSeekClient deepSeekClient,
                         ScheduleService scheduleService,
                         CourseService courseService,
                         TodoService todoService,
                         RecurringTodoService recurringTodoService,
                         PeriodConfigService periodConfigService,
                         ObjectMapper objectMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.actionMapper = actionMapper;
        this.deepSeekClient = deepSeekClient;
        this.scheduleService = scheduleService;
        this.courseService = courseService;
        this.todoService = todoService;
        this.recurringTodoService = recurringTodoService;
        this.periodConfigService = periodConfigService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isEnabled() {
        return deepSeekClient.isConfigured();
    }

    // ==================== 对话管理 ====================

    @Override
    public AiConversation createConversation(AiConversationRequest request, String apiKey) {
        AiConversation conversation = new AiConversation();
        conversation.setApiKey(apiKey);
        conversation.setScheduleId(request == null ? null : request.getScheduleId());
        String title = request == null ? null : request.getTitle();
        conversation.setTitle(
                title == null || title.isBlank() ? nextDefaultTitle(apiKey) : title.trim());
        conversationMapper.insert(conversation);
        return conversation;
    }

    /** 默认标题：日期 + 当天第几个对话（两位序号，从 01 开始），如 "2026-08-07 #01"。 */
    private String nextDefaultTitle(String apiKey) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getApiKey, apiKey)
                .ge(AiConversation::getCreatedAt, today.atStartOfDay())
                .lt(AiConversation::getCreatedAt, today.plusDays(1).atStartOfDay());
        long todayCount = conversationMapper.selectCount(wrapper);
        return String.format("%s #%02d", today.format(DateTimeFormatter.ISO_LOCAL_DATE), todayCount + 1);
    }

    @Override
    public AiConversation renameConversation(Long conversationId, String title, String apiKey) {
        AiConversation conversation = getConversation(conversationId, apiKey);
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (trimmed.length() > 100) {
            trimmed = trimmed.substring(0, 100);
        }
        conversation.setTitle(trimmed);
        conversationMapper.updateById(conversation);
        return conversation;
    }

    @Override
    public List<AiConversation> listConversations(String apiKey) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getApiKey, apiKey).orderByDesc(AiConversation::getUpdatedAt);
        return conversationMapper.selectList(wrapper);
    }

    @Override
    public void deleteConversation(Long conversationId, String apiKey) {
        getConversation(conversationId, apiKey);
        List<AiMessage> messages = loadMessages(conversationId);
        if (!messages.isEmpty()) {
            LambdaQueryWrapper<AiAction> actionWrapper = new LambdaQueryWrapper<>();
            actionWrapper.in(AiAction::getMessageId, messages.stream().map(AiMessage::getId).toList());
            actionMapper.delete(actionWrapper);
        }
        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getConversationId, conversationId);
        messageMapper.delete(wrapper);
        conversationMapper.deleteById(conversationId);
    }

    private AiConversation getConversation(Long conversationId, String apiKey) {
        AiConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "对话不存在");
        }
        return conversation;
    }

    private List<AiMessage> loadMessages(Long conversationId) {
        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getConversationId, conversationId).orderByAsc(AiMessage::getId);
        return messageMapper.selectList(wrapper);
    }

    private List<AiAction> loadActions(Long messageId) {
        LambdaQueryWrapper<AiAction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAction::getMessageId, messageId)
                .orderByAsc(AiAction::getSortOrder, AiAction::getId);
        return actionMapper.selectList(wrapper);
    }

    // ==================== 作用域与指纹 ====================

    /**
     * 按 action 类型判定作用域。
     * COURSE 类归课表域；TODO 与 RECURRING 类同归待办域（循环规则是待办的来源，语义相连）。
     */
    private String scopeOf(String actionType) {
        return actionType.contains("COURSE") ? SCOPE_COURSE : SCOPE_TODO;
    }

    /**
     * 计算指定作用域的数据指纹。课表与待办分别独立计算，
     * 因此改一个待办不会让课表相关的提案失效，反之亦然。
     */
    private String fingerprintOf(String scope, Long scheduleId, String apiKey) {
        StringBuilder sb = new StringBuilder();
        if (SCOPE_COURSE.equals(scope)) {
            sb.append("S").append(scheduleId).append('|');
            if (scheduleId != null) {
                for (Course c : courseService.listByScheduleId(scheduleId, apiKey)) {
                    sb.append(c.getId()).append(':').append(c.getName()).append(':')
                            .append(c.getDayOfWeek()).append(':').append(c.getStartPeriod()).append('-')
                            .append(c.getEndPeriod()).append(':').append(c.getWeeks()).append(':')
                            .append(c.getLocation()).append(':').append(c.getTeacher()).append(';');
                }
            }
        } else {
            for (Todo t : todoService.list(apiKey)) {
                sb.append(t.getId()).append(':').append(t.getTitle()).append(':')
                        .append(t.getDdl()).append(':').append(t.getCompleted()).append(';');
            }
            sb.append("|R|");
            for (RecurringTodo r : recurringTodoService.list(apiKey)) {
                sb.append(r.getId()).append(':').append(r.getTitle()).append(':')
                        .append(r.getFrequency()).append(':').append(r.getDayOfWeek()).append(':')
                        .append(r.getDayOfMonth()).append(':').append(r.getTriggerTime()).append(':')
                        .append(r.getScript()).append(':')
                        .append(r.getDdlOffsetMinutes()).append(':').append(r.getEnabled()).append(':')
                        .append(r.getChainAfterComplete()).append(';');
            }
        }
        return sha256(sb.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(500, "无法计算数据指纹");
        }
    }

    /**
     * 把该用户指定作用域内所有 PENDING 提案标记为 STALE。
     * scope 为 null 时表示两个作用域都失效。
     */
    private void invalidatePending(String apiKey, String scope, Long exceptActionId) {
        LambdaQueryWrapper<AiAction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAction::getApiKey, apiKey).eq(AiAction::getActionStatus, STATUS_PENDING);
        if (scope != null) {
            wrapper.eq(AiAction::getScope, scope);
        }
        if (exceptActionId != null) {
            wrapper.ne(AiAction::getId, exceptActionId);
        }
        for (AiAction stale : actionMapper.selectList(wrapper)) {
            stale.setActionStatus(STATUS_STALE);
            actionMapper.updateById(stale);
        }
    }

    @Override
    public void invalidateProposalsOnDataChange(String apiKey, String scope) {
        invalidatePending(apiKey, scope, null);
    }

    // ==================== 消息读取 ====================

    @Override
    public List<AiMessageResponse> listMessages(Long conversationId, String apiKey) {
        getConversation(conversationId, apiKey);
        List<AiMessageResponse> result = new ArrayList<>();
        for (AiMessage m : loadMessages(conversationId)) {
            result.add(toResponse(m));
        }
        return result;
    }

    private AiMessageResponse toResponse(AiMessage message) {
        AiMessageResponse response = new AiMessageResponse();
        response.setMessageId(message.getId());
        response.setRole(message.getRole());
        response.setText(message.getContent());
        response.setCreatedAt(message.getCreatedAt());

        List<AiActionDto> dtos = new ArrayList<>();
        for (AiAction action : loadActions(message.getId())) {
            JsonNode data = null;
            try {
                if (action.getActionData() != null) {
                    data = objectMapper.readTree(action.getActionData());
                }
            } catch (Exception e) {
                log.warn("Failed to parse stored action data of action {}", action.getId(), e);
            }
            dtos.add(new AiActionDto(action.getId(), action.getActionType(),
                    action.getActionStatus(), action.getScope(), data));
        }
        response.setActions(dtos);
        return response;
    }

    // ==================== 发送消息 ====================

    @Override
    @Transactional
    public AiMessageResponse sendMessage(Long conversationId, AiMessageRequest request, String apiKey) {
        return doSend(conversationId, request, apiKey, null);
    }

    @Override
    @Transactional
    public AiMessageResponse sendMessageStream(Long conversationId, AiMessageRequest request,
                                              String apiKey, StreamCallback callback) {
        return doSend(conversationId, request, apiKey, callback);
    }

    private AiMessageResponse doSend(Long conversationId, AiMessageRequest request,
                                     String apiKey, StreamCallback callback) {
        AiConversation conversation = getConversation(conversationId, apiKey);

        Long scheduleId = request.getScheduleId() != null ? request.getScheduleId() : conversation.getScheduleId();
        Schedule schedule = null;
        List<Course> courses = Collections.emptyList();
        if (scheduleId != null) {
            schedule = scheduleService.getById(scheduleId, apiKey);
            courses = courseService.listByScheduleId(scheduleId, apiKey);
        }
        List<Todo> todos = todoService.list(apiKey);
        List<RecurringTodo> recurringTodos = recurringTodoService.list(apiKey);
        List<PeriodConfig> periods = periodConfigService.list();

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole("user");
        userMessage.setContent(request.getContent());
        messageMapper.insert(userMessage);

        List<DeepSeekClient.Message> payload = new ArrayList<>();
        payload.add(new DeepSeekClient.Message("system",
                AiPromptBuilder.buildChatSystemPrompt(schedule, courses, todos, recurringTodos, periods, LocalDate.now())));

        List<AiMessage> history = loadMessages(conversationId);
        int from = Math.max(0, history.size() - HISTORY_LIMIT);
        for (int i = from; i < history.size(); i++) {
            AiMessage m = history.get(i);
            String content = m.getContent() == null ? "" : m.getContent();

            if (!"assistant".equals(m.getRole())) {
                payload.add(new DeepSeekClient.Message(m.getRole(), content));
                continue;
            }

            // assistant 历史必须还原成「模型当初本该输出的合法 JSON」。
            // 若在这里拼入自定义标注（如 [提案: ...]），模型会模仿该格式而不再输出 JSON。
            List<AiAction> acts = loadActions(m.getId());
            ObjectNode replay = objectMapper.createObjectNode();
            replay.put("text", content);
            ArrayNode actionsNode = replay.putArray("actions");
            for (AiAction a : acts) {
                ObjectNode an = actionsNode.addObject();
                an.put("type", a.getActionType());
                try {
                    an.set("data", objectMapper.readTree(
                            a.getActionData() == null ? "{}" : a.getActionData()));
                } catch (Exception e) {
                    an.set("data", objectMapper.createObjectNode());
                }
            }
            try {
                payload.add(new DeepSeekClient.Message("assistant",
                        objectMapper.writeValueAsString(replay)));
            } catch (Exception e) {
                payload.add(new DeepSeekClient.Message("assistant", content));
            }

            // 执行结果作为独立的 user 反馈告知，避免污染 assistant 的输出格式
            if (!acts.isEmpty()) {
                StringBuilder fb = new StringBuilder("[系统反馈] 上述提案的处理结果：");
                for (AiAction a : acts) {
                    fb.append("\n- ").append(a.getActionType())
                            .append(" → ").append(statusText(a.getActionStatus()));
                }
                fb.append("\n（这是系统信息，不是用户发言。请继续按 JSON 格式回复。）");
                payload.add(new DeepSeekClient.Message("user", fb.toString()));
            }
        }

        String raw;
        if (callback == null) {
            raw = deepSeekClient.chatJson(payload);
        } else {
            JsonTextStreamExtractor extractor = new JsonTextStreamExtractor();
            boolean[] planningNotified = {false};
            raw = deepSeekClient.chatJsonStream(payload, new DeepSeekClient.StreamListener() {
                @Override
                public void onReasoning(String delta) {
                    callback.onThinking(delta);
                }

                @Override
                public void onContent(String delta) {
                    String text = extractor.feed(delta);
                    if (!text.isEmpty()) {
                        callback.onText(text);
                    }
                    if (extractor.isTextComplete() && !planningNotified[0]) {
                        planningNotified[0] = true;
                        callback.onPlanning();
                    }
                }
            });
            if (!planningNotified[0]) {
                callback.onPlanning();
            }
        }

        JsonNode root = parseJson(raw);

        String text = root.path("text").asText("");
        if (text.isBlank()) {
            text = "（AI 未返回内容）";
        }

        AiMessage assistantMessage = new AiMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(text);
        messageMapper.insert(assistantMessage);

        // 兼容 actions 数组与旧的单个 action 字段
        List<JsonNode> actionNodes = new ArrayList<>();
        JsonNode actions = root.path("actions");
        if (actions.isArray()) {
            actions.forEach(actionNodes::add);
        }
        JsonNode single = root.path("action");
        if (single.isObject()) {
            actionNodes.add(single);
        }

        // 本次提案涉及的作用域，仅让这些作用域内的旧提案失效
        int order = 0;
        List<AiAction> created = new ArrayList<>();
        for (JsonNode node : actionNodes) {
            String type = node.path("type").asText("");
            if (type.isBlank()) {
                continue;
            }
            JsonNode data = node.path("data");
            if (data.isMissingNode()) {
                data = objectMapper.createObjectNode();
            }
            enrichBeforeSnapshot(type, data, courses, todos, recurringTodos);

            String scope = scopeOf(type);
            AiAction action = new AiAction();
            action.setMessageId(assistantMessage.getId());
            action.setApiKey(apiKey);
            action.setScope(scope);
            action.setActionType(type);
            action.setActionStatus(STATUS_PENDING);
            try {
                action.setActionData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                throw new BusinessException(502, "AI 返回的操作数据无法序列化");
            }
            action.setDataFingerprint(fingerprintOf(scope, scheduleId, apiKey));
            action.setSortOrder(order++);
            created.add(action);
        }

        // 先按作用域让旧提案失效，再插入新提案
        for (String scope : created.stream().map(AiAction::getScope).distinct().toList()) {
            invalidatePending(apiKey, scope, null);
        }
        created.forEach(actionMapper::insert);

        conversation.setScheduleId(scheduleId);
        conversationMapper.updateById(conversation);

        return toResponse(assistantMessage);
    }

    private String statusText(String status) {
        if (status == null) {
            return "待确认";
        }
        return switch (status) {
            case STATUS_EXECUTED -> "用户已确认执行，数据已生效";
            case STATUS_REJECTED -> "用户已取消，未执行";
            case STATUS_STALE -> "已失效（数据变化或有更新提案），未执行";
            default -> "待用户确认";
        };
    }

    private JsonNode parseJson(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            int start = text.indexOf('\n');
            int end = text.lastIndexOf("```");
            if (start > 0 && end > start) {
                text = text.substring(start + 1, end).trim();
            }
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception ignored) {
            // 兜底：尝试截取最外层花括号内容（模型偶尔会在 JSON 前后带解释文字）
            int first = text.indexOf('{');
            int last = text.lastIndexOf('}');
            if (first >= 0 && last > first) {
                try {
                    return objectMapper.readTree(text.substring(first, last + 1));
                } catch (Exception ignored2) {
                    // 继续走降级
                }
            }
            // 仍失败则降级为纯文本回复，不抛错中断对话
            log.warn("AI returned non-JSON content, degrading to plain text: {}", text);
            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("text", text.isBlank()
                    ? "抱歉，我没能正确组织回复，请再说一次。" : text);
            fallback.putArray("actions");
            return fallback;
        }
    }

    // ==================== 修改前快照 ====================

    private void enrichBeforeSnapshot(String type, JsonNode data, List<Course> courses,
                                      List<Todo> todos, List<RecurringTodo> rules) {
        if (!(data instanceof ObjectNode target)) {
            return;
        }
        ArrayNode before = objectMapper.createArrayNode();

        switch (type) {
            case "UPDATE_COURSE" -> {
                for (JsonNode node : data.path("courses")) {
                    if (!node.hasNonNull("id")) continue;
                    long id = node.get("id").asLong();
                    courses.stream().filter(c -> c.getId() == id).findFirst()
                            .ifPresent(c -> before.add(courseSnapshot(c)));
                }
            }
            case "DELETE_COURSE" -> {
                for (JsonNode node : data.path("courseIds")) {
                    long id = node.asLong();
                    courses.stream().filter(c -> c.getId() == id).findFirst()
                            .ifPresent(c -> before.add(courseSnapshot(c)));
                }
            }
            case "UPDATE_TODO" -> {
                for (JsonNode node : data.path("todos")) {
                    if (!node.hasNonNull("id")) continue;
                    long id = node.get("id").asLong();
                    todos.stream().filter(t -> t.getId() == id).findFirst()
                            .ifPresent(t -> before.add(todoSnapshot(t)));
                }
            }
            case "DELETE_TODO", "TOGGLE_TODO" -> {
                for (JsonNode node : data.path("todoIds")) {
                    long id = node.asLong();
                    todos.stream().filter(t -> t.getId() == id).findFirst()
                            .ifPresent(t -> before.add(todoSnapshot(t)));
                }
            }
            case "UPDATE_RECURRING" -> {
                for (JsonNode node : data.path("rules")) {
                    if (!node.hasNonNull("id")) continue;
                    long id = node.get("id").asLong();
                    rules.stream().filter(r -> r.getId() == id).findFirst()
                            .ifPresent(r -> before.add(recurringSnapshot(r)));
                }
            }
            case "DELETE_RECURRING", "TOGGLE_RECURRING" -> {
                for (JsonNode node : data.path("recurringIds")) {
                    long id = node.asLong();
                    rules.stream().filter(r -> r.getId() == id).findFirst()
                            .ifPresent(r -> before.add(recurringSnapshot(r)));
                }
            }
            default -> {
                return;
            }
        }

        if (!before.isEmpty()) {
            target.set("_before", before);
        }
    }

    private ObjectNode courseSnapshot(Course c) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", c.getId());
        node.put("name", c.getName());
        node.put("location", c.getLocation());
        node.put("teacher", c.getTeacher());
        node.put("dayOfWeek", c.getDayOfWeek());
        node.put("startPeriod", c.getStartPeriod());
        node.put("endPeriod", c.getEndPeriod());
        ArrayNode weeks = node.putArray("weeks");
        if (c.getWeeks() != null) {
            c.getWeeks().forEach(weeks::add);
        }
        return node;
    }

    private ObjectNode recurringSnapshot(RecurringTodo r) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", r.getId());
        node.put("title", r.getTitle());
        node.put("frequency", r.getFrequency());
        node.put("dayOfWeek", r.getDayOfWeek());
        node.put("dayOfMonth", r.getDayOfMonth());
        node.put("triggerTime", r.getTriggerTime() == null ? null : r.getTriggerTime().toString());
        node.put("script", r.getScript());
        node.put("ddlOffsetMinutes", r.getDdlOffsetMinutes());
        node.put("enabled", r.getEnabled());
        node.put("chainAfterComplete", r.getChainAfterComplete());
        return node;
    }

    private ObjectNode todoSnapshot(Todo t) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", t.getId());
        node.put("title", t.getTitle());
        node.put("ddl", t.getDdl() == null ? null : t.getDdl().toString());
        node.put("completed", t.getCompleted());
        return node;
    }

    // ==================== 执行 / 取消 ====================

    @Override
    @Transactional
    public AiMessageResponse executeAction(Long conversationId, Long actionId, String apiKey) {
        AiConversation conversation = getConversation(conversationId, apiKey);
        AiAction action = actionMapper.selectById(actionId);
        if (action == null || !action.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "操作不存在");
        }
        AiMessage message = messageMapper.selectById(action.getMessageId());
        if (message == null || !message.getConversationId().equals(conversationId)) {
            throw new BusinessException(404, "操作不存在");
        }
        if (STATUS_EXECUTED.equals(action.getActionStatus())) {
            throw new BusinessException(400, "该操作已执行");
        }
        if (STATUS_REJECTED.equals(action.getActionStatus())) {
            throw new BusinessException(400, "该操作已取消");
        }
        if (STATUS_STALE.equals(action.getActionStatus())) {
            throw new BusinessException(409, "该提案已失效（数据已变化或已有更新的提案），请重新向 AI 提问");
        }

        String currentFingerprint = fingerprintOf(action.getScope(), conversation.getScheduleId(), apiKey);
        if (action.getDataFingerprint() != null
                && !action.getDataFingerprint().equals(currentFingerprint)) {
            action.setActionStatus(STATUS_STALE);
            actionMapper.updateById(action);
            throw new BusinessException(409,
                    SCOPE_COURSE.equals(action.getScope())
                            ? "课表已发生变化，该提案已失效，请重新向 AI 提问"
                            : "待办已发生变化，该提案已失效，请重新向 AI 提问");
        }

        JsonNode data;
        try {
            data = objectMapper.readTree(action.getActionData() == null ? "{}" : action.getActionData());
        } catch (Exception e) {
            throw new BusinessException(500, "操作数据解析失败");
        }

        applyAction(action.getActionType(), data, conversation.getScheduleId(), apiKey);

        action.setActionStatus(STATUS_EXECUTED);
        actionMapper.updateById(action);

        // 同一作用域内其余 PENDING 提案基于旧数据，一并失效；
        // 但同一条消息里的兄弟提案（同批规划）保留，允许用户逐个执行。
        invalidateOtherScopePending(apiKey, action, conversation.getScheduleId());

        return toResponse(message);
    }

    /**
     * 执行后的失效处理。
     *
     * 判定依据是「提案是否基于执行前的旧数据」，而不是「是否属于同一条消息」：
     * - 比被执行提案更早生成（id 更小）的同作用域提案 → 基于旧数据，标记 STALE
     * - 与之同批、或在它之后生成（id 更大）的提案 → 生成时已看到更新后的数据，
     *   只需把指纹刷新到最新，仍可继续执行
     *
     * 这样既支持「一次规划多个操作、逐个确认」，也支持「执行完一条后 AI 又提了新提案、
     * 再回头执行」的顺序，不会误杀后生成的提案。
     *
     * 注意：MyBatis-Plus 的 updateById 会忽略 null 字段，因此必须写入重新计算出的
     * 指纹值，不能置 null。
     */
    private void invalidateOtherScopePending(String apiKey, AiAction executed, Long scheduleId) {
        LambdaQueryWrapper<AiAction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAction::getApiKey, apiKey)
                .eq(AiAction::getScope, executed.getScope())
                .eq(AiAction::getActionStatus, STATUS_PENDING);
        List<AiAction> others = actionMapper.selectList(wrapper);
        if (others.isEmpty()) {
            return;
        }
        String refreshed = fingerprintOf(executed.getScope(), scheduleId, apiKey);
        for (AiAction other : others) {
            boolean olderThanExecuted =
                    !other.getMessageId().equals(executed.getMessageId())
                            && other.getId() < executed.getId();
            if (olderThanExecuted) {
                other.setActionStatus(STATUS_STALE);
            } else {
                other.setDataFingerprint(refreshed);
            }
            actionMapper.updateById(other);
        }
    }

    @Override
    public AiMessageResponse rejectAction(Long conversationId, Long actionId, String apiKey) {
        getConversation(conversationId, apiKey);
        AiAction action = actionMapper.selectById(actionId);
        if (action == null || !action.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "操作不存在");
        }
        AiMessage message = messageMapper.selectById(action.getMessageId());
        if (message == null || !message.getConversationId().equals(conversationId)) {
            throw new BusinessException(404, "操作不存在");
        }
        if (STATUS_EXECUTED.equals(action.getActionStatus())) {
            throw new BusinessException(400, "该操作已执行，无法取消");
        }
        if (!STATUS_STALE.equals(action.getActionStatus())) {
            action.setActionStatus(STATUS_REJECTED);
            actionMapper.updateById(action);
        }
        return toResponse(message);
    }

    private void applyAction(String type, JsonNode data, Long scheduleId, String apiKey) {
        switch (type) {
            case "CREATE_COURSE" -> {
                requireSchedule(scheduleId);
                for (JsonNode node : arrayOf(data, "courses")) {
                    courseService.create(scheduleId, toCourseRequest(node), apiKey);
                }
            }
            case "UPDATE_COURSE" -> {
                requireSchedule(scheduleId);
                for (JsonNode node : arrayOf(data, "courses")) {
                    if (!node.hasNonNull("id")) {
                        throw new BusinessException(400, "修改课程缺少 id");
                    }
                    courseService.update(node.get("id").asLong(), toCourseRequest(node), apiKey);
                }
            }
            case "DELETE_COURSE" -> {
                for (JsonNode node : arrayOf(data, "courseIds")) {
                    courseService.delete(node.asLong(), apiKey);
                }
            }
            case "CREATE_TODO" -> {
                for (JsonNode node : arrayOf(data, "todos")) {
                    todoService.create(toTodoRequest(node), apiKey);
                }
            }
            case "UPDATE_TODO" -> {
                for (JsonNode node : arrayOf(data, "todos")) {
                    if (!node.hasNonNull("id")) {
                        throw new BusinessException(400, "修改待办缺少 id");
                    }
                    todoService.update(node.get("id").asLong(), toTodoRequest(node), apiKey);
                }
            }
            case "DELETE_TODO" -> {
                for (JsonNode node : arrayOf(data, "todoIds")) {
                    todoService.delete(node.asLong(), apiKey);
                }
            }
            case "TOGGLE_TODO" -> {
                for (JsonNode node : arrayOf(data, "todoIds")) {
                    todoService.toggleComplete(node.asLong(), apiKey);
                }
            }
            case "CREATE_RECURRING" -> {
                for (JsonNode node : arrayOf(data, "rules")) {
                    recurringTodoService.create(toRecurringRequest(node), apiKey);
                }
            }
            case "UPDATE_RECURRING" -> {
                for (JsonNode node : arrayOf(data, "rules")) {
                    if (!node.hasNonNull("id")) {
                        throw new BusinessException(400, "修改循环任务缺少 id");
                    }
                    recurringTodoService.update(node.get("id").asLong(), toRecurringRequest(node), apiKey);
                }
            }
            case "DELETE_RECURRING" -> {
                for (JsonNode node : arrayOf(data, "recurringIds")) {
                    recurringTodoService.delete(node.asLong(), apiKey);
                }
            }
            case "TOGGLE_RECURRING" -> {
                for (JsonNode node : arrayOf(data, "recurringIds")) {
                    recurringTodoService.toggleEnabled(node.asLong(), apiKey);
                }
            }
            default -> throw new BusinessException(400, "不支持的操作类型: " + type);
        }
    }

    private void requireSchedule(Long scheduleId) {
        if (scheduleId == null) {
            throw new BusinessException(400, "该操作需要先选择课表");
        }
    }

    private List<JsonNode> arrayOf(JsonNode data, String field) {
        JsonNode node = data.path(field);
        if (!node.isArray() || node.isEmpty()) {
            throw new BusinessException(400, "操作数据缺少 " + field);
        }
        List<JsonNode> list = new ArrayList<>();
        node.forEach(list::add);
        return list;
    }

    private CourseRequest toCourseRequest(JsonNode node) {
        CourseRequest req = new CourseRequest();
        req.setName(textOrNull(node, "name"));
        req.setLocation(textOrNull(node, "location"));
        req.setTeacher(textOrNull(node, "teacher"));
        req.setDayOfWeek(node.hasNonNull("dayOfWeek") ? node.get("dayOfWeek").asInt() : null);
        req.setStartPeriod(node.hasNonNull("startPeriod") ? node.get("startPeriod").asInt() : null);
        req.setEndPeriod(node.hasNonNull("endPeriod") ? node.get("endPeriod").asInt() : null);
        List<Integer> weeks = new ArrayList<>();
        node.path("weeks").forEach(w -> weeks.add(w.asInt()));
        req.setWeeks(weeks);
        if (req.getName() == null || req.getDayOfWeek() == null
                || req.getStartPeriod() == null || req.getEndPeriod() == null || weeks.isEmpty()) {
            throw new BusinessException(400, "课程信息不完整，无法执行");
        }
        return req;
    }

    private TodoRequest toTodoRequest(JsonNode node) {
        TodoRequest req = new TodoRequest();
        req.setTitle(textOrNull(node, "title"));
        String ddl = textOrNull(node, "ddl");
        if (req.getTitle() == null || ddl == null) {
            throw new BusinessException(400, "待办信息不完整，无法执行");
        }
        req.setDdl(parseDateTime(ddl));
        return req;
    }

    private RecurringTodoRequest toRecurringRequest(JsonNode node) {
        RecurringTodoRequest req = new RecurringTodoRequest();
        req.setTitle(textOrNull(node, "title"));
        String freq = textOrNull(node, "frequency");
        if (req.getTitle() == null || freq == null) {
            throw new BusinessException(400, "循环任务信息不完整，无法执行");
        }
        req.setFrequency(freq.toUpperCase());
        req.setDayOfWeek(node.hasNonNull("dayOfWeek") ? node.get("dayOfWeek").asInt() : null);
        req.setDayOfMonth(node.hasNonNull("dayOfMonth") ? node.get("dayOfMonth").asInt() : null);
        req.setScript(textOrNull(node, "script"));

        // CUSTOM 由脚本决定触发时机，不需要 triggerTime；其余频率必须有触发时刻
        if (!RecurringScheduleCalculator.CUSTOM.equals(req.getFrequency())) {
            String time = textOrNull(node, "triggerTime");
            if (time == null) {
                throw new BusinessException(400, "循环任务缺少触发时间");
            }
            req.setTriggerTime(parseTime(time));
        }

        if (!node.hasNonNull("ddlOffsetMinutes")) {
            throw new BusinessException(400, "循环任务缺少截止偏移时间");
        }
        req.setDdlOffsetMinutes(node.get("ddlOffsetMinutes").asInt());
        req.setChainAfterComplete(node.hasNonNull("chainAfterComplete")
                && node.get("chainAfterComplete").asBoolean());
        if (node.hasNonNull("enabled")) {
            req.setEnabled(node.get("enabled").asBoolean());
        }
        return req;
    }

    private LocalTime parseTime(String value) {
        String v = value.trim();
        for (String p : new String[]{"HH:mm:ss", "HH:mm", "H:mm"}) {
            try {
                return LocalTime.parse(v, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {
                // 尝试下一种格式
            }
        }
        throw new BusinessException(400, "无法解析触发时间: " + value);
    }

    private LocalDateTime parseDateTime(String value) {
        String v = value.trim().replace('T', ' ');
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                if ("yyyy-MM-dd".equals(p)) {
                    return LocalDate.parse(v, DateTimeFormatter.ofPattern(p)).atTime(23, 59);
                }
                return LocalDateTime.parse(v, DateTimeFormatter.ofPattern(p));
            } catch (Exception ignored) {
                // try next pattern
            }
        }
        throw new BusinessException(400, "无法解析截止时间: " + value);
    }

    private String textOrNull(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        String v = node.get(field).asText();
        return v == null || v.isBlank() || "null".equals(v) ? null : v;
    }

    // ==================== HTML 解析 ====================

    @Override
    public Map<String, Object> parseHtml(String html, Long scheduleId, String apiKey) {
        Schedule schedule = scheduleId == null ? null : scheduleService.getById(scheduleId, apiKey);
        List<PeriodConfig> periods = periodConfigService.list();

        String content = html.length() > HTML_MAX_LENGTH ? html.substring(0, HTML_MAX_LENGTH) : html;

        List<DeepSeekClient.Message> payload = new ArrayList<>();
        payload.add(new DeepSeekClient.Message("system", AiPromptBuilder.buildParseHtmlSystemPrompt(schedule, periods)));
        payload.add(new DeepSeekClient.Message("user", content));

        JsonNode root = parseJson(deepSeekClient.chatJson(payload));

        List<Map<String, Object>> courses = new ArrayList<>();
        for (JsonNode node : root.path("courses")) {
            Map<String, Object> course = new HashMap<>();
            course.put("name", textOrNull(node, "name"));
            course.put("location", textOrNull(node, "location"));
            course.put("teacher", textOrNull(node, "teacher"));
            course.put("dayOfWeek", node.hasNonNull("dayOfWeek") ? node.get("dayOfWeek").asInt() : null);
            course.put("startPeriod", node.hasNonNull("startPeriod") ? node.get("startPeriod").asInt() : null);
            course.put("endPeriod", node.hasNonNull("endPeriod") ? node.get("endPeriod").asInt() : null);
            List<Integer> weeks = new ArrayList<>();
            node.path("weeks").forEach(w -> weeks.add(w.asInt()));
            course.put("weeks", weeks);
            courses.add(course);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("courses", courses);
        result.put("note", root.path("note").asText(""));
        return result;
    }
}
