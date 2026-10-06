package com.ai.common.service.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StreamTokenEvent")
class StreamTokenEventTest {

  @Test
  @DisplayName("should keep the leading space of a token inside the JSON payload")
  void shouldKeepTheLeadingSpaceOfATokenInsideTheJsonPayload() {
    assertThat(StreamTokenEvent.toJson(" HER"))
        .isEqualTo("{\"type\":\"message\",\"token\":\" HER\"}");
  }

  @Test
  @DisplayName("should escape newlines so a token stays on one SSE data line")
  void shouldEscapeNewlinesSoATokenStaysOnOneSseDataLine() {
    assertThat(StreamTokenEvent.toJson("a\nb"))
        .isEqualTo("{\"type\":\"message\",\"token\":\"a\\nb\"}");
  }
}
