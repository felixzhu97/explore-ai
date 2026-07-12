package com.ai.common.util;

public final class LogSanitizer {

    private static final int DEFAULT_MAX_LENGTH = 50;

    private LogSanitizer() {
    }

    public static String truncate(String text) {
        return truncate(text, DEFAULT_MAX_LENGTH);
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
