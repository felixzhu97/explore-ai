package com.ai.plugin.domain.vo;

public enum PluginHealthStatus {
    UNKNOWN,
    HEALTHY,
    UNHEALTHY;

    public static PluginHealthStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return PluginHealthStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
