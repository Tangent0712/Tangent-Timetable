package com.timetable.service;

import java.util.List;

public interface DeepSeekClient {

    class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    /**
     * 流式回调。思考阶段与正文阶段分开通知，便于前端展示「思考中 → 输出 → 执行」三阶段。
     */
    interface StreamListener {
        /** 收到推理内容增量（thinking 阶段） */
        void onReasoning(String delta);

        /** 收到正文 JSON 增量 */
        void onContent(String delta);
    }

    /**
     * 以 JSON Mode 调用 DeepSeek，返回模型输出的原始字符串（应为 JSON 文本）。
     */
    String chatJson(List<Message> messages);

    /**
     * 流式调用，边接收边回调，最终返回拼接完成的完整原始输出。
     */
    String chatJsonStream(List<Message> messages, StreamListener listener);

    boolean isConfigured();
}
