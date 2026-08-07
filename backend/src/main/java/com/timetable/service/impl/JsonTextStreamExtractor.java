package com.timetable.service.impl;

/**
 * 从「正在流式生成的 JSON」中增量提取 text 字段的值。
 *
 * AI 以 JSON Mode 输出 {"text":"...","actions":[...]}，流式过程中拿到的是不完整的 JSON
 * 片段，无法直接解析。本类用小状态机扫描字符，只把 text 字段的字符串内容吐出来，
 * 使前端能在 text 生成过程中就逐字显示，text 结束后即可切换到「规划操作中」。
 */
public class JsonTextStreamExtractor {

    private enum State { SEEK_KEY, IN_KEY, AFTER_KEY, IN_VALUE, DONE }

    private State state = State.SEEK_KEY;
    private final StringBuilder keyBuffer = new StringBuilder();
    private boolean escaped = false;
    private boolean pendingUnicode = false;
    private final StringBuilder unicodeBuffer = new StringBuilder();

    /**
     * 喂入一段增量原始字符，返回本次新解析出的 text 明文（可能为空字符串）。
     */
    public String feed(String chunk) {
        if (state == State.DONE || chunk == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);

            switch (state) {
                case SEEK_KEY -> {
                    if (c == '"') {
                        keyBuffer.setLength(0);
                        state = State.IN_KEY;
                    }
                }
                case IN_KEY -> {
                    if (c == '"') {
                        state = "text".contentEquals(keyBuffer) ? State.AFTER_KEY : State.SEEK_KEY;
                    } else {
                        keyBuffer.append(c);
                    }
                }
                case AFTER_KEY -> {
                    if (c == '"') {
                        state = State.IN_VALUE;
                    } else if (c != ':' && !Character.isWhitespace(c)) {
                        // text 的值不是字符串（例如 null），放弃提取
                        state = State.SEEK_KEY;
                    }
                }
                case IN_VALUE -> {
                    if (pendingUnicode) {
                        unicodeBuffer.append(c);
                        if (unicodeBuffer.length() == 4) {
                            try {
                                out.append((char) Integer.parseInt(unicodeBuffer.toString(), 16));
                            } catch (NumberFormatException ignored) {
                                // 非法转义忽略
                            }
                            unicodeBuffer.setLength(0);
                            pendingUnicode = false;
                        }
                    } else if (escaped) {
                        switch (c) {
                            case 'n' -> out.append('\n');
                            case 't' -> out.append('\t');
                            case 'r' -> out.append('\r');
                            case 'b' -> out.append('\b');
                            case 'f' -> out.append('\f');
                            case 'u' -> pendingUnicode = true;
                            default -> out.append(c);
                        }
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        state = State.DONE;
                    } else {
                        out.append(c);
                    }
                }
                case DONE -> {
                    return out.toString();
                }
            }
        }
        return out.toString();
    }

    /** text 字段是否已完整输出（可据此切换到「规划操作中」） */
    public boolean isTextComplete() {
        return state == State.DONE;
    }
}
