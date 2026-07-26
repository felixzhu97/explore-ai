package com.ai.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityConfig")
class ObservabilityConfigTest {

    private ObservabilityConfig config;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        config = new ObservabilityConfig();
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    @DisplayName("should_register_chat_metrics")
    void should_register_chat_metrics() {
        Counter requests = config.chatRequestCounter(meterRegistry);
        Counter errors = config.chatErrorCounter(meterRegistry);
        Timer latency = config.chatLatencyTimer(meterRegistry);

        requests.increment();
        errors.increment();
        latency.record(java.time.Duration.ofMillis(12));

        assertThat(meterRegistry.get("ai.chat.requests").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("ai.chat.errors").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("ai.chat.latency").timer().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should_register_rag_and_tool_metrics")
    void should_register_rag_and_tool_metrics() {
        config.ragRequestCounter(meterRegistry).increment();
        config.ragRetrievalCounter(meterRegistry).increment();
        config.ragLatencyTimer(meterRegistry).record(java.time.Duration.ofMillis(5));
        config.toolCallCounter(meterRegistry).increment();

        assertThat(meterRegistry.get("ai.rag.requests").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("ai.rag.retrievals").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("ai.rag.latency").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("ai.tool.calls").counter().count()).isEqualTo(1.0);
    }
}
