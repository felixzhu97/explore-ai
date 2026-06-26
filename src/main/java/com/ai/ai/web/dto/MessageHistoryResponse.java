package com.ai.ai.web.dto;

import com.ai.ai.domain.model.ChatMessage;
import com.ai.ai.domain.model.ChatSession;

import java.time.Instant;
import java.util.List;

/**
 * Message history response DTO.
 */
public record MessageHistoryResponse(
    String sessionId,
    List<MessageDto> messages,
    int totalCount
) {
    public static MessageHistoryResponse from(ChatSession session) {
        return new MessageHistoryResponse(
            session.getId().toString(),
            session.getMessages().stream().map(MessageDto::from).toList(),
            session.getMessageCount()
        );
    }
}

/**
 * Message DTO.
 */
record MessageDto(
    String id,
    String text,
    String role,
    Instant timestamp
) {
    public static MessageDto from(ChatMessage message) {
        return new MessageDto(
            message.getId().toString(),
            message.getText(),
            message.getRole().value(),
            message.getTimestamp()
        );
    }
}
