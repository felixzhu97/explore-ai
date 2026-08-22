package com.ai.metrics.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.ai.metrics.domain", "com.ai.base.domain", "com.ai.common.domain"})
@EnableJpaRepositories(basePackageClasses = SpringDataAiInvocationEventRepository.class)
class AiInvocationEventJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_KEY = "c:77777777-7777-7777-7777-777777777777";

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataAiInvocationEventRepository repository;

  @Test
  @DisplayName("should persist and reload invocation event when round tripping")
  void shouldPersistAndReloadInvocationEventWhenRoundTripping() {
    AiInvocationEvent event =
        AiInvocationEvent.builder()
            .domain(AiDomain.CHAT)
            .operation("completion")
            .outcome(InvocationOutcome.SUCCESS)
            .latencyMs(250)
            .provider("openai")
            .model("gpt-4")
            .ownerKey(OWNER_KEY)
            .build();

    repository.saveAndFlush(event);
    em.clear();

    Optional<AiInvocationEvent> reloaded = repository.findById(event.getId());

    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getOperation()).isEqualTo("completion");
    assertThat(reloaded.get().getLatencyMs()).isEqualTo(250);
  }

  @Test
  @DisplayName("should store domain and outcome values when persisting event")
  void shouldStoreDomainAndOutcomeValuesWhenPersistingEvent() {
    AiInvocationEvent event =
        AiInvocationEvent.builder()
            .domain(AiDomain.RAG)
            .operation("embed")
            .outcome(InvocationOutcome.ERROR)
            .latencyMs(90)
            .errorCode("timeout")
            .ownerKey(OWNER_KEY)
            .build();

    repository.saveAndFlush(event);
    em.clear();

    AiInvocationEvent reloaded = repository.findById(event.getId()).orElseThrow();

    assertThat(reloaded.getDomain()).isEqualTo(AiDomain.RAG);
    assertThat(reloaded.getOutcome()).isEqualTo(InvocationOutcome.ERROR);
    assertThat(reloaded.getErrorCode()).isEqualTo("timeout");
  }

  @Test
  @DisplayName("should store owner key when assigned before persist")
  void shouldStoreOwnerKeyWhenAssignedBeforePersist() {
    AiInvocationEvent event =
        AiInvocationEvent.builder()
            .domain(AiDomain.AGENTS)
            .operation("run")
            .outcome(InvocationOutcome.SUCCESS)
            .latencyMs(10)
            .build();
    event.assignOwnerKey(OWNER_KEY);

    repository.saveAndFlush(event);
    em.clear();

    String rawOwnerKey =
        (String)
            em.getEntityManager()
                .createNativeQuery("SELECT owner_key FROM ai_invocation_events WHERE id = ?")
                .setParameter(1, event.getId().value())
                .getSingleResult();

    assertThat(rawOwnerKey).isEqualTo(OWNER_KEY);
  }

  @Test
  @DisplayName("should preserve occurred at timestamp when round tripping event")
  void shouldPreserveOccurredAtTimestampWhenRoundTrippingEvent() {
    Instant occurredAt = Instant.parse("2026-03-15T12:00:00Z");
    AiInvocationEvent event =
        AiInvocationEvent.builder()
            .occurredAt(occurredAt)
            .domain(AiDomain.WORKFLOW)
            .operation("execute")
            .outcome(InvocationOutcome.SUCCESS)
            .latencyMs(500)
            .ownerKey(OWNER_KEY)
            .build();

    repository.saveAndFlush(event);
    em.clear();

    AiInvocationEvent reloaded = repository.findById(event.getId()).orElseThrow();

    assertThat(reloaded.getOccurredAt()).isEqualTo(occurredAt);
  }
}
