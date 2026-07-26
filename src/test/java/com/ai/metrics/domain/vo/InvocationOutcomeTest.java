package com.ai.metrics.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InvocationOutcome")
class InvocationOutcomeTest {

    @Test
    @DisplayName("should_parse_success_and_error_when_valid")
    void should_parse_success_and_error_when_valid() {
        assertThat(InvocationOutcome.parse("SUCCESS")).isEqualTo(InvocationOutcome.SUCCESS);
        assertThat(InvocationOutcome.parse(" error ")).isEqualTo(InvocationOutcome.ERROR);
    }

    @Test
    @DisplayName("should_throw_when_outcome_blank_or_unknown")
    void should_throw_when_outcome_blank_or_unknown() {
        assertThatThrownBy(() -> InvocationOutcome.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InvocationOutcome.parse(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> InvocationOutcome.parse("failed"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown outcome");
    }
}
