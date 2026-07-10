package com.ai.chat.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record ChatStreamRequest(
    List<ChatMessageDto> messages,
    @JsonAlias("session_id") String sessionId,
    String provider,
    String model,
    @JsonAlias("tools_enabled") Boolean toolsEnabled
) {
    public record ChatMessageDto(String role, String content) {}
}
