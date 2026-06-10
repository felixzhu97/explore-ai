package com.ai.agents.presentation.dto;

import com.ai.agents.domain.AgentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * DTO for agent requests.
 * Supports both single message format and messages array format (for SSE streaming).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentRequestDto(
        // Single message format
        @JsonProperty("message") String message,

        @NotNull(message = "Agent type is required")
        @JsonProperty("agentType") AgentType agentType,

        @JsonProperty("sessionId") String sessionId,

        @JsonProperty("topK") Integer topK,

        @JsonProperty("model") String model,

        @JsonProperty("metadata") Map<String, Object> metadata,

        // Messages array format (for SSE streaming compatibility with Angular)
        @JsonProperty("messages") List<ChatMessage> messages
) {
    /**
     * Chat message for messages array format.
     */
    public record ChatMessage(
            String role,
            String content
    ) {}

    public AgentRequestDto {
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    /**
     * Get the user message content, preferring messages array over single message.
     */
    public String getUserMessage() {
        // First try messages array
        if (messages != null && !messages.isEmpty()) {
            return messages.stream()
                    .filter(m -> "user".equalsIgnoreCase(m.role()))
                    .map(ChatMessage::content)
                    .findFirst()
                    .orElse(messages.get(0).content());
        }
        // Fall back to single message
        return message;
    }

    public static AgentRequestDto of(String message, AgentType agentType) {
        return new AgentRequestDto(message, agentType, null, null, null, null, null);
    }

    public static AgentRequestDto of(String message, AgentType agentType, String sessionId) {
        return new AgentRequestDto(message, agentType, sessionId, null, null, null, null);
    }

    public static AgentRequestDto fromMessages(List<ChatMessage> messages, AgentType agentType) {
        return new AgentRequestDto(null, agentType, null, null, null, null, messages);
    }
}
