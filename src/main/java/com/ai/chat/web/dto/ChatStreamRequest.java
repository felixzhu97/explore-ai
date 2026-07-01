package com.ai.chat.web.dto;

import java.util.List;

public record ChatStreamRequest(
    List<ChatMessageDto> messages,
    String session_id,
    String provider,
    String model
) {
    public record ChatMessageDto(String role, String content) {}
}
