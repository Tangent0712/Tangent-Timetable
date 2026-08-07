package com.timetable.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 宽松的 LocalDateTime 反序列化：同时接受
 * "2026-09-30T10:00:00" / "2026-09-30 10:00:00" / "2026-09-30 10:00" / "2026-09-30"，
 * 避免各端日期格式差异导致 400/500。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter[] PATTERNS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
    };

    @Bean
    public SimpleModule lenientDateTimeModule() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String value = p.getText();
                if (value == null || value.isBlank()) {
                    return null;
                }
                String trimmed = value.trim();
                for (DateTimeFormatter fmt : PATTERNS) {
                    try {
                        return LocalDateTime.parse(trimmed, fmt);
                    } catch (Exception ignored) {
                        // 尝试下一种格式
                    }
                }
                try {
                    return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atTime(23, 59);
                } catch (Exception ignored) {
                    // 落到统一报错
                }
                throw new IOException("无法解析日期时间: " + value);
            }
        });
        // 序列化统一输出 ISO 格式，保证各端解析一致
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        return module;
    }
}
