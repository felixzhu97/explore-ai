package com.ai.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability configuration for Spring AI metrics.
 * Configures Micrometer metrics and Prometheus export for AI service monitoring.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    @Qualifier("chatRequestCounter")
    public Counter chatRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.chat.requests")
                .description("Total number of chat requests")
                .tag("type", "chat")
                .register(meterRegistry);
    }

    @Bean
    @Qualifier("chatErrorCounter")
    public Counter chatErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.chat.errors")
                .description("Total number of chat errors")
                .tag("type", "chat")
                .register(meterRegistry);
    }

    @Bean
    @Qualifier("chatLatencyTimer")
    public Timer chatLatencyTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ai.chat.latency")
                .description("Chat request latency")
                .tag("type", "chat")
                .register(meterRegistry);
    }

    @Bean
    @Qualifier("ragRequestCounter")
    public Counter ragRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.rag.requests")
                .description("Total number of RAG requests")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    @Bean
    @Qualifier("ragRetrievalCounter")
    public Counter ragRetrievalCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.rag.retrievals")
                .description("Total number of document retrievals")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    @Bean
    @Qualifier("ragLatencyTimer")
    public Timer ragLatencyTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ai.rag.latency")
                .description("RAG request latency")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    @Bean
    @Qualifier("toolCallCounter")
    public Counter toolCallCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.tool.calls")
                .description("Total number of tool calls")
                .tag("type", "tool")
                .register(meterRegistry);
    }
}
