package com.ai.chat.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Chat request DTO for API requests. */
public record ChatRequest(
    @NotBlank @Size(max = 10000, message = "Message cannot exceed 10000 characters") String message,
    String sessionId) {
  public ChatRequest {
    if (message != null) {
      message = message.trim();
    }
  }

  /** Creates a request without a session. */
  public static ChatRequest of(String message) {
    return new ChatRequest(message, null);
  }

  /** Creates a request for a session. */
  public static ChatRequest of(String message, String sessionId) {
    return new ChatRequest(message, sessionId);
  }
}
