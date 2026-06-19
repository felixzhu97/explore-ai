package com.ai.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability configuration for Spring AI metrics.
 * Configures Micrometer metrics and Prometheus export for AI service monitoring.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public Counter chatRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.chat.requests")
                .description("Total number of chat requests")
                .tag("type", "chat")
                .register(meterRegistry);
    }

    @Bean
    public Counter chatErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.chat.errors")
                .description("Total number of chat errors")
                .tag("type", "chat")
                .register(meterRegistry);
    }

    @Bean
    public Timer chatLatencyTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ai.chat.latency")
                .description("Chat request latency")
                .tag("type", "chat")
                .register(meterRegistry);
    }

    @Bean
    public Counter ragRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.rag.requests")
                .description("Total number of RAG requests")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    @Bean
    public Counter ragRetrievalCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.rag.retrievals")
                .description("Total number of document retrievals")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    @Bean
    public Timer ragLatencyTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ai.rag.latency")
                .description("RAG request latency")
                .tag("type", "rag")
                .register(meterRegistry);
    }

    @Bean
    public Counter toolCallCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.tool.calls")
                .description("Total number of tool calls")
                .tag("type", "tool")
                .register(meterRegistry);
    }
}
