package com.ai.common.service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SSE {@code data} for one streamed model token. Raw tokens lose their leading space because SSE
 * strips one space after {@code data:}, so tokens travel as JSON.
 */
public final class StreamTokenEvent {

  private static final ObjectMapper JSON = new ObjectMapper();

  private record Payload(String type, String token) {}

  private StreamTokenEvent() {}

  /** Returns {@code {"type":"message","token":...}} for the token. */
  public static String json(String token) {
    try {
      return JSON.writeValueAsString(new Payload("message", token));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to encode stream token", e);
    }
  }
}
