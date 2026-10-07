package com.ai.metrics.service;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.ErrorSummary;
import com.ai.metrics.domain.vo.Latency;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Records AI invocation events for metrics dashboards without failing the business path. */
@Service
@RequiredArgsConstructor
public class AiInvocationRecorder {

  private final AiInvocationEventRepository eventRepository;
  private final MeterRegistry meterRegistry;

  /** Persists the event and updates Micrometer meters; failures are swallowed, never thrown. */
  public void record(AiInvocationEvent event) {
    try {
      eventRepository.save(event);
      meterRegistry
          .counter(
              "ai.invocations",
              "domain",
              event.getDomain().value(),
              "outcome",
              event.getOutcome().value(),
              "operation",
              event.getOperation())
          .increment();
      meterRegistry
          .timer(
              "ai.invocation.latency",
              "domain",
              event.getDomain().value(),
              "outcome",
              event.getOutcome().value())
          .record(Duration.ofMillis(event.getLatencyMs()));
    } catch (Exception expected) {
    }
  }

  /** Records a successful invocation with its latency, provider, model, and session. */
  public void recordSuccess(
      AiDomain domain,
      String operation,
      Latency latency,
      OwnerKey owner,
      String provider,
      String model,
      String sessionId) {
    record(
        AiInvocationEvent.succeeded(domain, operation, latency, owner)
            .provider(provider)
            .model(model)
            .sessionId(sessionId)
            .build());
  }

  /** Records a failed invocation with a normalized summary of the error. */
  public void recordError(
      AiDomain domain,
      String operation,
      Latency latency,
      OwnerKey owner,
      String provider,
      String model,
      String sessionId,
      Throwable error) {
    record(
        AiInvocationEvent.failed(domain, operation, latency, owner, ErrorSummary.of(error))
            .provider(provider)
            .model(model)
            .sessionId(sessionId)
            .build());
  }
}
