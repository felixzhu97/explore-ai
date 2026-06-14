package com.ai.infrastructure.adapter.tool.chat;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.vo.ChatSessionId;
import com.ai.infrastructure.adapter.persistence.InMemoryChatSessionRepository;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import com.ai.infrastructure.adapter.tool.JsonSchemaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tool for getting chat history of a session.
 */
@Component
public class GetChatHistoryTool implements ToolProvider {

    private final InMemoryChatSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    public GetChatHistoryTool(InMemoryChatSessionRepository sessionRepository, ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.stringProp("sessionId", "The chat session ID", true),
            JsonSchemaBuilder.integerProp("limit", "Maximum number of recent messages (default 50)", false)
        );
        return ToolDefinition.atomic(
            "get_chat_history",
            "Get the message history for a specific chat session.",
            JsonSchemaBuilder.objectSchema(List.of("sessionId"), props),
            "chat"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        String sessionIdStr = invocation.getArg("sessionId", "");
        int limit = invocation.getArg("limit", 50);

        if (sessionIdStr == null || sessionIdStr.isBlank()) {
            return ToolResult.error("sessionId is required");
        }

        try {
            ChatSessionId sessionId = ChatSessionId.of(sessionIdStr);
            Optional<ChatSession> sessionOpt = sessionRepository.findById(sessionId);

            if (sessionOpt.isEmpty()) {
                return ToolResult.error("Session not found: " + sessionIdStr);
            }

            ChatSession session = sessionOpt.get();
            List<ChatMessage> messages = session.getRecentMessages(limit);

            List<Map<String, Object>> msgList = messages.stream()
                .map(this::toMap)
                .toList();

            Map<String, Object> structured = new LinkedHashMap<>();
            structured.put("sessionId", sessionIdStr);
            structured.put("title", session.getTitle());
            structured.put("totalMessages", session.getMessageCount());
            structured.put("returnedMessages", msgList.size());
            structured.put("messages", msgList);

            String content = objectMapper.writeValueAsString(structured);
            return ToolResult.success(content, structured);
        } catch (IllegalArgumentException e) {
            return ToolResult.error("Invalid session ID format: " + sessionIdStr);
        } catch (Exception e) {
            return ToolResult.error("Failed to get chat history: " + e.getMessage());
        }
    }

    private Map<String, Object> toMap(ChatMessage msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", msg.getRole());
        m.put("text", msg.getText());
        m.put("timestamp", msg.getTimestamp().toString());
        return m;
    }
}
