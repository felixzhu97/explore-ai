package com.ai.infrastructure.adapter.tool.chat;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.ChatSession;
import com.ai.infrastructure.adapter.persistence.InMemoryChatSessionRepository;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import com.ai.infrastructure.adapter.tool.JsonSchemaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for listing all chat sessions.
 */
@Component
public class ListSessionsTool implements ToolProvider {

    private final InMemoryChatSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    public ListSessionsTool(InMemoryChatSessionRepository sessionRepository, ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.integerProp("limit", "Maximum number of sessions (default 20)", false),
            JsonSchemaBuilder.integerProp("offset", "Number of sessions to skip (default 0)", false)
        );
        return ToolDefinition.atomic(
            "list_sessions",
            "List all chat sessions with optional pagination.",
            JsonSchemaBuilder.objectSchema(List.of(), props),
            "chat"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        int limit = invocation.getArg("limit", 20);
        int offset = invocation.getArg("offset", 0);

        try {
            List<ChatSession> allSessions = sessionRepository.findAll();
            int total = allSessions.size();

            List<ChatSession> page = allSessions.stream()
                .sorted((a, b) -> b.getLastActivityAt().compareTo(a.getLastActivityAt()))
                .skip(offset)
                .limit(limit)
                .toList();

            List<Map<String, Object>> sessionList = page.stream()
                .map(this::toMap)
                .toList();

            Map<String, Object> structured = new LinkedHashMap<>();
            structured.put("total", total);
            structured.put("offset", offset);
            structured.put("limit", limit);
            structured.put("sessions", sessionList);

            String content = objectMapper.writeValueAsString(structured);
            return ToolResult.success(content, structured);
        } catch (Exception e) {
            return ToolResult.error("Failed to list sessions: " + e.getMessage());
        }
    }

    private Map<String, Object> toMap(ChatSession session) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", session.getId().value());
        m.put("title", session.getTitle());
        m.put("messageCount", session.getMessageCount());
        m.put("createdAt", session.getCreatedAt().toString());
        m.put("lastActivityAt", session.getLastActivityAt().toString());
        return m;
    }
}
