package com.timetable.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekProperties {
    private String key;
    private String baseUrl = "https://api.deepseek.com";
    /** 官方模型名 deepseek-flash（模型版本 DeepSeek-V4.1-Flash） */
    private String model = "deepseek-flash";
    private int timeoutSeconds = 120;
    /** 输出 token 上限（含推理），防止长时间思考把预算耗尽导致正文为空 */
    private int maxTokens = 16384;
    /** 推理强度：low / medium / high。低强度可显著缩短思考时间 */
    private String reasoningEffort = "low";

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getReasoningEffort() { return reasoningEffort; }
    public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
}
