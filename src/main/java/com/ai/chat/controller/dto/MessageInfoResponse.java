package com.ai.chat.controller.dto;

import com.ai.chat.domain.model.ChatMessage;
import java.time.Instant;
import java.util.List;

public record MessageInfoResponse(
    String id, ChatRole role, String content, Instant timestamp, List<WebSourceResponse> sources) {
  /** Maps a message to a response without web sources. */
  public static MessageInfoResponse from(ChatMessage message) {
    return from(message, List.of());
  }

  /** Builds a response from the message, omitting sources when the list is null or empty. */
  public static MessageInfoResponse from(ChatMessage message, List<WebSourceResponse> sources) {
    List<WebSourceResponse> safeSources =
        sources == null || sources.isEmpty() ? null : List.copyOf(sources);
    return new MessageInfoResponse(
        message.getId().toString(),
        ChatRole.of(message.getMessageType()),
        message.getText(),
        message.getTimestamp(),
        safeSources);
  }
}
