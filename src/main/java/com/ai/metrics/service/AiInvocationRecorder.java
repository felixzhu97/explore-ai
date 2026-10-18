package com.ai.metrics.service;

import com.ai.common.domain.event.AiInvocationCompletedEvent;
import com.ai.common.domain.event.AiInvocationFailedEvent;
import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.ErrorSummary;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Records AI invocation events for metrics dashboards from Domain Events, without failing the
 * business path that published them.
 */
@Service
@RequiredArgsConstructor
public class AiInvocationRecorder {

  private final AiInvocationEventRepository eventRepository;
  private final MeterRegistry meterRegistry;

  @EventListener
  void handleAiInvocationCompletedEvent(AiInvocationCompletedEvent event) {
    recordInvocation(
        () ->
            AiInvocationEvent.createSucceededEvent(
                    AiCapability.parseCapability(event.capability()),
                    event.operation(),
                    Latency.createFromMillis(event.latencyMs()),
                    event.owner())
                .provider(event.provider())
                .model(event.model())
                .sessionId(event.sessionId())
                .documentId(event.documentId())
                .agentType(event.agentType())
                .build());
  }

  @EventListener
  void handleAiInvocationFailedEvent(AiInvocationFailedEvent event) {
    recordInvocation(
        () ->
            AiInvocationEvent.createFailedEvent(
                    AiCapability.parseCapability(event.capability()),
                    event.operation(),
                    Latency.createFromMillis(event.latencyMs()),
                    event.owner(),
                    ErrorSummary.createSummary(event.errorCode(), event.errorMessage()))
                .provider(event.provider())
                .model(event.model())
                .sessionId(event.sessionId())
                .documentId(event.documentId())
                .agentType(event.agentType())
                .build());
  }

  /** Persists the event and updates Micrometer meters; failures are swallowed, never thrown. */
  private void recordInvocation(Supplier<AiInvocationEvent> factory) {
    try {
      AiInvocationEvent event = factory.get();
      eventRepository.save(event);
      meterRegistry
          .counter(
              "ai.invocations",
              "capability",
              event.getCapability().getValue(),
              "outcome",
              event.getOutcome().getValue(),
              "operation",
              event.getOperation())
          .increment();
      meterRegistry
          .timer(
              "ai.invocation.latency",
              "capability",
              event.getCapability().getValue(),
              "outcome",
              event.getOutcome().getValue())
          .record(Duration.ofMillis(event.getLatencyMs()));
    } catch (Exception expected) {
    }
  }
}
