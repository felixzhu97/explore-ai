package com.ai.chat.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotEmpty @Valid List<Message> messages,
    String sessionId,
    String provider,
    String model,
    Boolean toolsEnabled,
    List<String> skillIds) {

  /** A single chat message with role and content. */
  public record Message(@NotNull ChatRole role, String content) {}
}
