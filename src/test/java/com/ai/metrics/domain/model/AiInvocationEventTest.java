package com.ai.metrics.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.domain.model.OwnerKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiInvocationEvent")
class AiInvocationEventTest {

  private static final OwnerKey OWNER = OwnerKey.forClient("11111111-1111-4111-8111-111111111111");

  @Test
  @DisplayName("should record success without error when built from succeeded")
  void shouldRecordSuccessWithoutErrorWhenBuiltFromSucceeded() {
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(
                AiCapability.TOOLS, " tools.weather ", Latency.ofMillis(-5), OWNER)
            .provider("  ")
            .model("gpt")
            .tokens(new TokenUsage(3, 4))
            .build();

    assertThat(event.getOutcome()).isEqualTo(InvocationOutcome.SUCCESS);
    assertThat(event.getOperation()).isEqualTo("tools.weather");
    assertThat(event.latency()).isEqualTo(Latency.ofMillis(0));
    assertThat(event.getProvider()).isNull();
    assertThat(event.getModel()).isEqualTo("gpt");
    assertThat(event.getPromptTokens()).isEqualTo(3);
    assertThat(event.getCompletionTokens()).isEqualTo(4);
    assertThat(event.getOwnerKey()).isEqualTo(OWNER);
    assertThat(event.error()).isEmpty();
  }

  @Test
  @DisplayName("should carry sanitized error when built from failed")
  void shouldCarrySanitizedErrorWhenBuiltFromFailed() {
    AiInvocationEvent event =
        AiInvocationEvent.failed(
                AiCapability.CHAT,
                "chat.stream",
                Latency.ofMillis(12),
                OWNER,
                ErrorSummary.of("Timeout", "line one\nline two" + "x".repeat(600)))
            .build();

    assertThat(event.getOutcome()).isEqualTo(InvocationOutcome.ERROR);
    assertThat(event.getErrorCode()).isEqualTo("Timeout");
    assertThat(event.getErrorMessage()).startsWith("line one line two").hasSize(512);
    assertThat(event.error()).map(ErrorSummary::code).contains("Timeout");
  }

  @Test
  @DisplayName("should reject blank operation when starting an event")
  void shouldRejectBlankOperationWhenStartingAnEvent() {
    assertThatThrownBy(
            () -> AiInvocationEvent.succeeded(AiCapability.CHAT, " ", Latency.ofMillis(1), OWNER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("operation");
  }

  @Test
  @DisplayName("should require owner when starting an event")
  void shouldRequireOwnerWhenStartingAnEvent() {
    assertThatThrownBy(
            () -> AiInvocationEvent.succeeded(AiCapability.CHAT, "chat", Latency.ofMillis(1), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ownerKey");
  }

  @Test
  @DisplayName("should require error when starting a failed event")
  void shouldRequireErrorWhenStartingAFailedEvent() {
    assertThatThrownBy(
            () ->
                AiInvocationEvent.failed(
                    AiCapability.CHAT, "chat", Latency.ofMillis(1), OWNER, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("error");
  }
}
