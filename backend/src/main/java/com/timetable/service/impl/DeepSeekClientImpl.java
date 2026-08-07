package com.timetable.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.timetable.config.DeepSeekProperties;
import com.timetable.exception.BusinessException;
import com.timetable.service.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
public class DeepSeekClientImpl implements DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClientImpl.class);

    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekClientImpl(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Override
    public boolean isConfigured() {
        String key = properties.getKey();
        return key != null && !key.isBlank() && !"your-deepseek-api-key".equals(key);
    }

    @Override
    public String chatJson(List<Message> messages) {
        return execute(messages, null);
    }

    @Override
    public String chatJsonStream(List<Message> messages, StreamListener listener) {
        return execute(messages, listener);
    }

    private ObjectNode buildBody(List<Message> messages, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("temperature", 0.2);
        ArrayNode arr = body.putArray("messages");
        for (Message m : messages) {
            ObjectNode node = arr.addObject();
            node.put("role", m.getRole());
            node.put("content", m.getContent());
        }
        body.putObject("response_format").put("type", "json_object");
        body.put("max_tokens", properties.getMaxTokens());
        if (properties.getReasoningEffort() != null && !properties.getReasoningEffort().isBlank()) {
            body.put("reasoning_effort", properties.getReasoningEffort());
        }
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    private String execute(List<Message> messages, StreamListener listener) {
        if (!isConfigured()) {
            throw new BusinessException(503, "未配置 DeepSeek API Key，AI 功能不可用");
        }
        boolean stream = listener != null;
        ObjectNode body = buildBody(messages, stream);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            if (!stream) {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    log.error("DeepSeek API error {}: {}", response.statusCode(), response.body());
                    throw new BusinessException(502, "AI 服务调用失败: HTTP " + response.statusCode());
                }
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode content = root.path("choices").path(0).path("message").path("content");
                if (content.isMissingNode() || content.isNull()) {
                    throw new BusinessException(502, "AI 服务返回内容为空");
                }
                return content.asText();
            }

            HttpResponse<java.io.InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(502, "AI 服务调用失败: HTTP " + response.statusCode());
            }

            StringBuilder full = new StringBuilder();
            String finishReason = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if (payload.isEmpty() || "[DONE]".equals(payload)) {
                        continue;
                    }
                    JsonNode chunk;
                    try {
                        chunk = objectMapper.readTree(payload);
                    } catch (Exception e) {
                        continue;
                    }
                    JsonNode choice = chunk.path("choices").path(0);
                    JsonNode delta = choice.path("delta");
                    JsonNode reasoning = delta.path("reasoning_content");
                    if (reasoning.isTextual() && !reasoning.asText().isEmpty()) {
                        listener.onReasoning(reasoning.asText());
                    }
                    JsonNode content = delta.path("content");
                    if (content.isTextual() && !content.asText().isEmpty()) {
                        full.append(content.asText());
                        listener.onContent(content.asText());
                    }
                    if (choice.path("finish_reason").isTextual()) {
                        finishReason = choice.path("finish_reason").asText();
                    }
                }
            }
            if (full.length() == 0) {
                log.error("DeepSeek returned no content, finish_reason={}", finishReason);
                if ("length".equals(finishReason)) {
                    throw new BusinessException(502,
                            "AI 思考过长导致未能给出结论，请把指令拆分得更简单一些再试");
                }
                throw new BusinessException(502, "AI 服务返回内容为空，请重试");
            }
            return full.toString();
        } catch (BusinessException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(502, "AI 服务调用被中断");
        } catch (Exception e) {
            log.error("DeepSeek API request failed", e);
            throw new BusinessException(502, "AI 服务调用失败: " + e.getMessage());
        }
    }
}
