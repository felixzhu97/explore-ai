package com.ai.metrics.application;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Records AI invocation events for metrics dashboards without failing the business path.
 */
@Service
public class AiInvocationRecorder {

    private static final Logger log = LoggerFactory.getLogger(AiInvocationRecorder.class);

    private final AiInvocationEventRepository eventRepository;
    private final MeterRegistry meterRegistry;

    public AiInvocationRecorder(AiInvocationEventRepository eventRepository, MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.meterRegistry = meterRegistry;
    }

    public void record(AiInvocationEvent event) {
        try {
            eventRepository.save(event);
            meterRegistry.counter(
                    "ai.invocations",
                    "domain", event.getDomain().value(),
                    "outcome", event.getOutcome().value(),
                    "operation", event.getOperation()
            ).increment();
            meterRegistry.timer(
                    "ai.invocation.latency",
                    "domain", event.getDomain().value(),
                    "outcome", event.getOutcome().value()
            ).record(java.time.Duration.ofMillis(event.getLatencyMs()));
        } catch (Exception ex) {
            log.warn("Failed to record AI invocation event domain={} operation={}",
                    event.getDomain().value(), event.getOperation(), ex);
        }
    }

    public void recordSuccess(
            AiDomain domain,
            String operation,
            long latencyMs,
            String provider,
            String model,
            String sessionId) {
        record(AiInvocationEvent.builder()
                .domain(domain)
                .operation(operation)
                .outcome(InvocationOutcome.SUCCESS)
                .latencyMs(latencyMs)
                .provider(provider)
                .model(model)
                .sessionId(sessionId)
                .build());
    }

    public void recordError(
            AiDomain domain,
            String operation,
            long latencyMs,
            String provider,
            String model,
            String sessionId,
            String errorCode,
            String errorMessage) {
        record(AiInvocationEvent.builder()
                .domain(domain)
                .operation(operation)
                .outcome(InvocationOutcome.ERROR)
                .latencyMs(latencyMs)
                .provider(provider)
                .model(model)
                .sessionId(sessionId)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build());
    }
}
