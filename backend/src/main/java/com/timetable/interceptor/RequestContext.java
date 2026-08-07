package com.timetable.interceptor;

public class RequestContext {
    private static final ThreadLocal<String> API_KEY_HOLDER = new ThreadLocal<>();

    public static void setApiKey(String apiKey) {
        API_KEY_HOLDER.set(apiKey);
    }

    public static String getApiKey() {
        return API_KEY_HOLDER.get();
    }

    public static void clear() {
        API_KEY_HOLDER.remove();
    }
}
