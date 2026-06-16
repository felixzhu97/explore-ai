package com.ai.application.tool;

import java.util.Map;

/**
 * Represents a tool invocation request.
 */
public final class ToolInvocation {

    private final String toolName;
    private final Map<String, Object> arguments;
    private final String sessionId;

    public ToolInvocation(String toolName, Map<String, Object> arguments) {
        this(toolName, arguments, null);
    }

    public ToolInvocation(String toolName, Map<String, Object> arguments, String sessionId) {
        this.toolName = toolName;
        this.arguments = arguments != null ? arguments : Map.of();
        this.sessionId = sessionId;
    }

    public String toolName() { return toolName; }
    public Map<String, Object> arguments() { return arguments; }
    public String sessionId() { return sessionId; }

    @SuppressWarnings("unchecked")
    public <T> T getArg(String key, T defaultValue) {
        Object val = arguments.get(key);
        if (val == null) return defaultValue;
        if (defaultValue instanceof Integer && val instanceof Number) {
            return (T) Integer.valueOf(((Number) val).intValue());
        }
        if (defaultValue instanceof Double && val instanceof Number) {
            return (T) Double.valueOf(((Number) val).doubleValue());
        }
        if (defaultValue instanceof String && val instanceof String) {
            return (T) val;
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        return "ToolInvocation{toolName='" + toolName + "', args=" + arguments + ", sessionId=" + sessionId + "}";
    }
}
