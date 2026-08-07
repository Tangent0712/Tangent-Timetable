package com.timetable.dto;

import jakarta.validation.constraints.NotBlank;

public class AiParseHtmlRequest {
    @NotBlank(message = "HTML 内容不能为空")
    private String html;

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }
}
