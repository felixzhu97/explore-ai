package com.ai.a2a.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/** HTTP+JSON body for {@code POST /a2a/v1/message:send} and {@code message:stream}. */
public record A2aSendMessageRequest(@NotNull @Valid A2aMessageBody message) {

  /** Nested A2A Message fields. */
  public record A2aMessageBody(
      @NotNull String messageId,
      String role,
      List<Map<String, Object>> parts,
      Map<String, Object> metadata) {}
}
