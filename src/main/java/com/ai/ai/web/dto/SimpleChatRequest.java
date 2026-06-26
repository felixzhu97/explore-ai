package com.ai.ai.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Simple chat request DTO for legacy API compatibility.
 */
@Deprecated
public record SimpleChatRequest(
    @NotBlank String message,
    @NotBlank String userId
) {}
