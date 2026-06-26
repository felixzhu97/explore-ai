package com.ai.ai.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Create session request DTO.
 */
public record CreateSessionRequest(
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    String title
) {
    public static CreateSessionRequest of(String title) {
        return new CreateSessionRequest(title);
    }
}
