package com.timetable.service;

import com.timetable.dto.AiConversationRequest;
import com.timetable.dto.AiMessageRequest;
import com.timetable.dto.AiMessageResponse;
import com.timetable.entity.AiConversation;

import java.util.List;
import java.util.Map;

public interface AiService {

    AiConversation createConversation(AiConversationRequest request, String apiKey);

    AiConversation renameConversation(Long conversationId, String title, String apiKey);

    List<AiConversation> listConversations(String apiKey);

    void deleteConversation(Long conversationId, String apiKey);

    List<AiMessageResponse> listMessages(Long conversationId, String apiKey);

    AiMessageResponse sendMessage(Long conversationId, AiMessageRequest request, String apiKey);

    /**
     * 流式发送消息。阶段事件通过 listener 推送，返回最终落库的完整消息。
     */
    AiMessageResponse sendMessageStream(Long conversationId, AiMessageRequest request,
                                       String apiKey, StreamCallback callback);

    interface StreamCallback {
        /** 思考阶段增量 */
        void onThinking(String delta);

        /** 正文增量 */
        void onText(String delta);

        /** 正文结束，开始解析操作提案 */
        void onPlanning();
    }

    AiMessageResponse executeAction(Long conversationId, Long actionId, String apiKey);

    AiMessageResponse rejectAction(Long conversationId, Long actionId, String apiKey);

    /**
     * 当用户手动改动课表/待办后调用，使对应作用域内待确认的 AI 提案失效。
     * scope 为 COURSE 或 TODO，两者互不影响。
     */
    void invalidateProposalsOnDataChange(String apiKey, String scope);

    /**
     * 解析教务系统 HTML，返回 {"courses": [...]} 结构供前端预览。
     */
    Map<String, Object> parseHtml(String html, Long scheduleId, String apiKey);

    boolean isEnabled();
}
