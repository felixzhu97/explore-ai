package com.ai.chat.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Streaming chat request body.
 *
 * @param messages conversation messages
 * @param sessionId optional session id
 * @param provider optional LLM provider
 * @param model optional model id
 * @param toolsEnabled whether tools are enabled
 * @param skillIds optional skill ids
 */
public record ChatStreamRequest(
    @NotEmpty List<Message> messages,
    String sessionId,
    String provider,
    String model,
    Boolean toolsEnabled,
    List<String> skillIds) {

  /** A single chat message with role and content. */
  public record Message(String role, String content) {}
}
