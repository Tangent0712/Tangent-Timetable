package com.timetable.dto;

public class AuthVerifyResponse {
    private boolean valid;
    private String label;
    private String avatarUrl;

    public AuthVerifyResponse() {}

    public AuthVerifyResponse(boolean valid, String label) {
        this.valid = valid;
        this.label = label;
    }

    public AuthVerifyResponse(boolean valid, String label, String avatarUrl) {
        this.valid = valid;
        this.label = label;
        this.avatarUrl = avatarUrl;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
