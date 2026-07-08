package com.ai.chat.web.dto;

import com.ai.chat.domain.model.ChatMessage;

import java.time.Instant;

public record MessageInfoResponse(
    String id,
    String role,
    String content,
    Instant timestamp
) {
    public static MessageInfoResponse from(ChatMessage message) {
        return new MessageInfoResponse(
                message.getId().toString(),
                message.role(),
                message.getText(),
                message.getTimestamp()
        );
    }
}
