package com.ai.chat.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatStreamRequest(
    @NotEmpty(message = "Messages cannot be empty")
    List<@NotNull(message = "Message cannot be null") @Valid ChatMessageDto> messages,
    @JsonProperty("session_id")
    String sessionId,
    String provider,
    String model
) {
    public record ChatMessageDto(
        @NotBlank(message = "Role cannot be blank")
        String role,
        @NotBlank(message = "Content cannot be blank")
        @Size(max = 10000, message = "Content cannot exceed 10000 characters")
        String content
    ) {}
}
