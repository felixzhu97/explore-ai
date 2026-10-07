package com.ai.chat.controller.dto;

import com.ai.chat.domain.model.ChatMessage;
import java.time.Instant;

/** Chat response DTO for API responses. */
public record ChatResponse(String response, String sessionId, String messageId, Instant timestamp) {
  /** Creates a response for a session message. */
  public static ChatResponse createResponse(String response, String sessionId, String messageId) {
    return new ChatResponse(response, sessionId, messageId, Instant.now());
  }

  /** Creates a response without a session. */
  public static ChatResponse createResponse(String response) {
    return new ChatResponse(response, null, null, Instant.now());
  }

  /** Maps a stored chat message to a response. */
  public static ChatResponse fromMessage(ChatMessage message, String sessionId) {
    return new ChatResponse(
        message.getText(), sessionId, message.getId().toString(), message.getTimestamp());
  }
}
