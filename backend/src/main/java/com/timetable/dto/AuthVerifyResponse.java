package com.timetable.dto;

public class AuthVerifyResponse {
    private boolean valid;
    private String label;

    public AuthVerifyResponse() {}

    public AuthVerifyResponse(boolean valid, String label) {
        this.valid = valid;
        this.label = label;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
