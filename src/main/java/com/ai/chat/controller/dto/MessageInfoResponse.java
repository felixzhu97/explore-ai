package com.ai.chat.controller.dto;

import com.ai.chat.domain.model.ChatMessage;
import java.time.Instant;
import java.util.List;

public record MessageInfoResponse(
    String id, String role, String content, Instant timestamp, List<WebSourceDto> sources) {
  public static MessageInfoResponse from(ChatMessage message) {
    return from(message, List.of());
  }

  /** Builds a response from the message, omitting sources when the list is null or empty. */
  public static MessageInfoResponse from(ChatMessage message, List<WebSourceDto> sources) {
    List<WebSourceDto> safeSources =
        sources == null || sources.isEmpty() ? null : List.copyOf(sources);
    return new MessageInfoResponse(
        message.getId().toString(),
        message.role(),
        message.getText(),
        message.getTimestamp(),
        safeSources);
  }
}
