package com.ai.common.infra.llm;

import com.ai.common.service.llm.ToolCallEvent;
import com.ai.common.service.llm.ToolResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.lang.Nullable;

/**
 * Wraps a {@link ToolCallback} and emits SSE-friendly tool_call / tool_result events. Binds {@link
 * ToolEventChannel} to {@code conversationId} on the tool execution thread.
 */
@RequiredArgsConstructor
public final class NotifyingToolCallback implements ToolCallback {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final ToolCallback delegate;
  private final String conversationId;

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
    ToolEventChannel.setCurrentSessionId(conversationId);
    try {
      String name = getToolDefinition().name();
      ToolEventChannel.publishEvent(toJson(ToolCallEvent.createEvent(name, toolInput)));
      try {
        String result =
            toolContext == null ? delegate.call(toolInput) : delegate.call(toolInput, toolContext);
        ToolEventChannel.publishEvent(
            toJson(ToolResultEvent.createSuccessEvent(name, truncateText(result))));
        return result;
      } catch (RuntimeException e) {
        String message = e.getMessage() == null ? "tool failed" : e.getMessage();
        ToolEventChannel.publishEvent(toJson(ToolResultEvent.createFailureEvent(name, message)));
        throw e;
      }
    } finally {
      ToolEventChannel.clearCurrentSessionId();
    }
  }

  private static String truncateText(String value) {
    if (value == null) {
      return "";
    }
    return value.length() <= 500 ? value : value.substring(0, 500) + "...";
  }

  private static String toJson(Record payload) {
    try {
      return JSON.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      return "{\"type\":\"tool_result\",\"ok\":false,\"output\":\"serialize error\"}";
    }
  }
}
