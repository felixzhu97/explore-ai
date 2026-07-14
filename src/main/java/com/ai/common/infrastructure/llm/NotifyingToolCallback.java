package com.ai.common.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wraps a {@link ToolCallback} and emits SSE-friendly tool_call / tool_result events.
 */
public final class NotifyingToolCallback implements ToolCallback {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ToolCallback delegate;

    public NotifyingToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return callAndNotify(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return callAndNotify(toolInput, toolContext);
    }

    private String callAndNotify(String toolInput, @Nullable ToolContext toolContext) {
        String name = getToolDefinition().name();
        ToolEventChannel.publish(toJson(Map.of(
                "type", "tool_call",
                "name", name,
                "input", toolInput == null ? "" : toolInput)));
        try {
            String result = toolContext == null
                    ? delegate.call(toolInput)
                    : delegate.call(toolInput, toolContext);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "tool_result");
            payload.put("name", name);
            payload.put("ok", true);
            payload.put("output", truncate(result));
            ToolEventChannel.publish(toJson(payload));
            return result;
        } catch (RuntimeException e) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "tool_result");
            payload.put("name", name);
            payload.put("ok", false);
            payload.put("output", e.getMessage() == null ? "tool failed" : e.getMessage());
            ToolEventChannel.publish(toJson(payload));
            throw e;
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "...";
    }

    private static String toJson(Map<String, ?> payload) {
        try {
            return JSON.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"type\":\"tool_result\",\"ok\":false,\"output\":\"serialize error\"}";
        }
    }
}
