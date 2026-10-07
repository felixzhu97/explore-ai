package com.ai.metrics.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvocationOutcome")
class InvocationOutcomeTest {

  @Test
  @DisplayName("should parse success and error when valid")
  void shouldParseSuccessAndErrorWhenValid() {
    assertThat(InvocationOutcome.parseOutcome("SUCCESS")).isEqualTo(InvocationOutcome.SUCCESS);
    assertThat(InvocationOutcome.parseOutcome(" error ")).isEqualTo(InvocationOutcome.ERROR);
  }

  @Test
  @DisplayName("should throw when outcome blank or unknown")
  void shouldThrowWhenOutcomeBlankOrUnknown() {
    assertThatThrownBy(() -> InvocationOutcome.parseOutcome(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> InvocationOutcome.parseOutcome(" "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> InvocationOutcome.parseOutcome("failed"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown outcome");
  }
}
