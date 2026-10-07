package com.ai.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiDomain;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AiInvocationRecorder")
class AiInvocationRecorderTest {

  private static final OwnerKey OWNER = OwnerKey.forClient("11111111-1111-4111-8111-111111111111");

  @Test
  @DisplayName("should persist event when repository succeeds")
  void shouldPersistEventWhenRepositorySucceeds() {
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

    recorder.recordSuccess(
        AiDomain.CHAT, "chat.stream", Latency.ofMillis(15), OWNER, "openai", "gpt", "s1");

    assertThat(saved).hasSize(1);
    assertThat(saved.getFirst().getOutcome()).isEqualTo(InvocationOutcome.SUCCESS);
    assertThat(saved.getFirst().getOwnerKey()).isEqualTo(OWNER);
  }

  @Test
  @DisplayName("should not throw when repository fails")
  void shouldNotThrowWhenRepositoryFails() {
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
                recorder.recordError(
                    AiDomain.RAG,
                    "rag.chat",
                    Latency.ofMillis(20),
                    OWNER,
                    "openai",
                    null,
                    null,
                    new IllegalStateException("boom")))
        .doesNotThrowAnyException();
  }
}
