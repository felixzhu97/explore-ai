package com.ai.metrics.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiCapability")
class AiCapabilityTest {

  @Test
  @DisplayName("should parse known capability when case insensitive")
  void shouldParseKnownCapabilityWhenCaseInsensitive() {
    assertThat(AiCapability.parse("CHAT")).contains(AiCapability.CHAT);
    assertThat(AiCapability.parse(" rag ")).contains(AiCapability.RAG);
  }

  @Test
  @DisplayName("should return empty when capability blank or unknown")
  void shouldReturnEmptyWhenCapabilityBlankOrUnknown() {
    assertThat(AiCapability.parse(null)).isEmpty();
    assertThat(AiCapability.parse("   ")).isEmpty();
    assertThat(AiCapability.parse("unknown")).isEmpty();
  }

  @Test
  @DisplayName("should throw when require unknown capability")
  void shouldThrowWhenRequireUnknownCapability() {
    assertThatThrownBy(() -> AiCapability.require("nope"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown AI capability");
  }
}
