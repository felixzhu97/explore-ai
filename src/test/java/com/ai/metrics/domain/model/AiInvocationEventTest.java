package com.ai.metrics.domain.model;

import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AiInvocationEvent")
class AiInvocationEventTest {

    @Test
    @DisplayName("should_normalize_blank_optional_fields_and_clamp_latency")
    void should_normalize_blank_optional_fields_and_clamp_latency() {
        AiInvocationEvent event = AiInvocationEvent.builder()
                .domain(AiDomain.TOOLS)
                .operation(" tools.weather ")
                .outcome(InvocationOutcome.SUCCESS)
                .latencyMs(-5)
                .provider("  ")
                .model("gpt")
                .errorMessage("x".repeat(600))
                .build();

        assertThat(event.getOperation()).isEqualTo("tools.weather");
        assertThat(event.getLatencyMs()).isZero();
        assertThat(event.getProvider()).isNull();
        assertThat(event.getModel()).isEqualTo("gpt");
        assertThat(event.getErrorMessage()).hasSize(512);
    }

    @Test
    @DisplayName("should_reject_blank_operation_when_building")
    void should_reject_blank_operation_when_building() {
        assertThatThrownBy(() -> AiInvocationEvent.builder()
                .domain(AiDomain.CHAT)
                .operation(" ")
                .outcome(InvocationOutcome.SUCCESS)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operation");
    }
}
