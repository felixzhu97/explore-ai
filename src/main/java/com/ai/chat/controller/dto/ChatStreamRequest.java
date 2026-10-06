package com.ai.chat.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotEmpty @Size(max = ChatStreamRequest.MAX_MESSAGES) @Valid List<Message> messages,
    @Size(max = 100) String sessionId,
    @Size(max = 50) String provider,
    @Size(max = 100) String model,
    Boolean toolsEnabled,
    @Size(max = 20) List<String> skillIds) {

  public static final int MAX_MESSAGES = 200;
  public static final int MAX_CONTENT_LENGTH = 32_000;

  /** A single chat message with role and content. */
  public record Message(@NotNull ChatRole role, @Size(max = MAX_CONTENT_LENGTH) String content) {}
}
