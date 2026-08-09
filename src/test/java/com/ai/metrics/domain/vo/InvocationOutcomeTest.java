package com.ai.metrics.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InvocationOutcome")
class InvocationOutcomeTest {

    @Test
    @DisplayName("should parse success and error when valid")
    void shouldParseSuccessAndErrorWhenValid() {
        assertThat(InvocationOutcome.parse("SUCCESS")).isEqualTo(InvocationOutcome.SUCCESS);
        assertThat(InvocationOutcome.parse(" error ")).isEqualTo(InvocationOutcome.ERROR);
    }

    @Test
    @DisplayName("should throw when outcome blank or unknown")
    void shouldThrowWhenOutcomeBlankOrUnknown() {
        assertThatThrownBy(() -> InvocationOutcome.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InvocationOutcome.parse(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InvocationOutcome.parse("failed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown outcome");
    }
}
