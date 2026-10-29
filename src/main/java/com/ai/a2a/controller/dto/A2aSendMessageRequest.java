package com.ai.a2a.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** HTTP+JSON body for {@code POST /a2a/v1/message:send} and {@code message:stream}. */
public record A2aSendMessageRequest(@NotNull @Valid A2aMessageBody message) {

  /** Nested A2A Message fields. */
  public record A2aMessageBody(
      @NotNull String messageId,
      String role,
      List<A2aMessagePart> parts,
      A2aMessageMetadata metadata) {}

  /** One message part (text is used for Agent invoke). */
  public record A2aMessagePart(String text) {}

  /** Optional routing metadata for catalog Agent selection. */
  public record A2aMessageMetadata(String agentType, String skill) {}
}
