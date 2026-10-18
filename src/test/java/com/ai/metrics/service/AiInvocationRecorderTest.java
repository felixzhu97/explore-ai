package com.ai.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ai.common.domain.event.AiInvocationCompletedEvent;
import com.ai.common.domain.event.AiInvocationFailedEvent;
import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiInvocationRecorder")
class AiInvocationRecorderTest {

  private static final OwnerKey OWNER =
      OwnerKey.createClientKey("11111111-1111-4111-8111-111111111111");

  @Test
  @DisplayName("should persist event when invocation completed event is handled")
  void shouldPersistEventWhenInvocationCompletedEventIsHandled() {
    List<AiInvocationEvent> saved = new ArrayList<>();
    AiInvocationRecorder recorder =
        new AiInvocationRecorder(
            new AiInvocationEventRepository() {
              @Override
              public void save(AiInvocationEvent event) {
                saved.add(event);
              }

              @Override
              public PageResult findDrilldown(DrilldownQuery query) {
                return new PageResult(List.of(), 0);
              }
            },
            new SimpleMeterRegistry());

    recorder.handleAiInvocationCompletedEvent(
        new AiInvocationCompletedEvent(
            "chat", "chat.stream", 15, OWNER, "openai", "gpt", "s1", null, null));

    assertThat(saved).hasSize(1);
    assertThat(saved.getFirst().getCapability()).isEqualTo(AiCapability.CHAT);
    assertThat(saved.getFirst().getLatencyMs()).isEqualTo(15);
    assertThat(saved.getFirst().getOutcome()).isEqualTo(InvocationOutcome.SUCCESS);
    assertThat(saved.getFirst().getOwnerKey()).isEqualTo(OWNER);
  }

  @Test
  @DisplayName("should not throw when repository fails while handling a failed event")
  void shouldNotThrowWhenRepositoryFailsWhileHandlingAFailedEvent() {
    AiInvocationRecorder recorder =
        new AiInvocationRecorder(
            new AiInvocationEventRepository() {
              @Override
              public void save(AiInvocationEvent event) {
                throw new IllegalStateException("db down");
              }

              @Override
              public PageResult findDrilldown(DrilldownQuery query) {
                return new PageResult(List.of(), 0);
              }
            },
            new SimpleMeterRegistry());

    assertThatCode(
            () ->
                recorder.handleAiInvocationFailedEvent(
                    new AiInvocationFailedEvent(
                        "rag",
                        "rag.chat",
                        20,
                        OWNER,
                        "openai",
                        null,
                        null,
                        null,
                        null,
                        "IllegalStateException",
                        "boom")))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should not throw when event names an unknown capability")
  void shouldNotThrowWhenEventNamesAnUnknownCapability() {
    List<AiInvocationEvent> saved = new ArrayList<>();
    AiInvocationRecorder recorder =
        new AiInvocationRecorder(
            new AiInvocationEventRepository() {
              @Override
              public void save(AiInvocationEvent event) {
                saved.add(event);
              }

              @Override
              public PageResult findDrilldown(DrilldownQuery query) {
                return new PageResult(List.of(), 0);
              }
            },
            new SimpleMeterRegistry());

    assertThatCode(
            () ->
                recorder.handleAiInvocationCompletedEvent(
                    new AiInvocationCompletedEvent(
                        "unknown", "x", 1, OWNER, null, null, null, null, null)))
        .doesNotThrowAnyException();
    assertThat(saved).isEmpty();
  }
}
