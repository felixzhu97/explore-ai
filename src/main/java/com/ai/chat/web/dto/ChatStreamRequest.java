package com.ai.chat.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    List<ChatMessageDto> messages,
    @JsonAlias("session_id") String sessionId,
    String provider,
    String model,
    @JsonAlias("tools_enabled") Boolean toolsEnabled,
    @JsonAlias("skill_ids") List<String> skillIds) {

  /** A single chat message with role and content. */
  public record ChatMessageDto(String role, String content) {}
}
