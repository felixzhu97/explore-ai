package com.ai.metrics.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.ErrorSummary;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.repository.AiInvocationEventRepository.DrilldownQuery;
import com.ai.metrics.domain.repository.AiInvocationEventRepository.PageResult;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaAiInvocationEventRepository.class)
class AiInvocationEventJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_KEY = "c:77777777-7777-7777-7777-777777777777";
  private static final OwnerKey OWNER = OwnerKey.parse(OWNER_KEY);

  @Autowired private JpaAiInvocationEventRepository repository;

  @Test
  @DisplayName("should persist and reload invocation event when round tripping")
  void shouldPersistAndReloadInvocationEventWhenRoundTripping() {
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(AiCapability.CHAT, "completion", Latency.ofMillis(250), OWNER)
            .provider("openai")
            .model("gpt-4")
            .build();

    em.persistAndFlush(event);
    em.clear();

    AiInvocationEvent reloaded = em.find(AiInvocationEvent.class, event.getId());

    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getOperation()).isEqualTo("completion");
    assertThat(reloaded.getLatencyMs()).isEqualTo(250);
    assertThat(reloaded.getOwnerKey().value()).isEqualTo(OWNER_KEY);
    assertThat(rawColumn(event, "owner_key")).isEqualTo(OWNER_KEY);
  }

  @Test
  @DisplayName("should store capability and outcome values when persisting event")
  void shouldStoreCapabilityAndOutcomeValuesWhenPersistingEvent() {
    AiInvocationEvent event =
        AiInvocationEvent.failed(
                AiCapability.RAG,
                "embed",
                Latency.ofMillis(90),
                OWNER,
                ErrorSummary.of("timeout", null))
            .build();

    em.persistAndFlush(event);
    em.clear();

    AiInvocationEvent reloaded = em.find(AiInvocationEvent.class, event.getId());

    assertThat(reloaded.getCapability()).isEqualTo(AiCapability.RAG);
    assertThat(reloaded.getOutcome()).isEqualTo(InvocationOutcome.ERROR);
    assertThat(reloaded.getErrorCode()).isEqualTo("timeout");
  }

  @Test
  @DisplayName("should store lowercase capability and outcome when persisting event")
  void shouldStoreLowercaseCapabilityAndOutcomeWhenPersistingEvent() {
    AiInvocationEvent event =
        AiInvocationEvent.failed(
                AiCapability.VISION,
                "describe",
                Latency.ofMillis(5),
                OWNER,
                ErrorSummary.of("unknown", null))
            .build();

    em.persistAndFlush(event);
    em.clear();

    assertThat(rawColumn(event, "capability")).isEqualTo("vision");
    assertThat(rawColumn(event, "outcome")).isEqualTo("error");
  }

  @Test
  @DisplayName("should preserve occurred at timestamp when round tripping event")
  void shouldPreserveOccurredAtTimestampWhenRoundTrippingEvent() {
    Instant occurredAt = Instant.parse("2026-03-15T12:00:00Z");
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(AiCapability.WORKFLOW, "execute", Latency.ofMillis(500), OWNER)
            .occurredAt(occurredAt)
            .build();

    em.persistAndFlush(event);
    em.clear();

    AiInvocationEvent reloaded = em.find(AiInvocationEvent.class, event.getId());

    assertThat(reloaded.getOccurredAt()).isEqualTo(occurredAt);
  }

  @Test
  @DisplayName("should filter and read occurred at as instant when querying through jdbc")
  void shouldFilterAndReadOccurredAtAsInstantWhenQueryingThroughJdbc() {
    Instant occurredAt = Instant.parse("2026-03-15T12:00:00.250Z");
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(AiCapability.WORKFLOW, "execute", Latency.ofMillis(500), OWNER)
            .occurredAt(occurredAt)
            .build();
    em.persistAndFlush(event);
    em.clear();
    PageResult page =
        repository.findDrilldown(
            new DrilldownQuery(
                Optional.of(AiCapability.WORKFLOW),
                Optional.of(occurredAt),
                Optional.of(occurredAt.plusMillis(1)),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                10));

    assertThat(page.items())
        .singleElement()
        .extracting(AiInvocationEvent::getOccurredAt)
        .isEqualTo(occurredAt);
  }

  @Test
  @DisplayName("should clamp the drilldown to the first page of at most 100 events")
  void shouldClampTheDrilldownToTheFirstPageOfAtMost100Events() {
    for (int i = 0; i < 101; i++) {
      em.persist(event("s" + i, Instant.parse("2026-03-15T12:00:00Z").plusSeconds(i)));
    }
    flushAndClear();

    PageResult page = repository.findDrilldown(query(-2, 500));

    assertThat(page.total()).isEqualTo(101);
    assertThat(page.items()).hasSize(100);
    assertThat(page.items().getFirst().getSessionId()).isEqualTo("s100");
  }

  @Test
  @DisplayName("should delete events of the sessions and those older than the cutoff")
  void shouldDeleteEventsOfTheSessionsAndThoseOlderThanTheCutoff() {
    em.persist(event("erased", Instant.parse("2026-03-15T12:00:00Z")));
    em.persist(event("old", Instant.parse("2025-01-01T00:00:00Z")));
    em.persist(event("kept", Instant.parse("2026-03-15T12:00:00Z")));
    flushAndClear();

    assertThat(repository.deleteBySessionIds(List.of("erased", " "))).isEqualTo(1);
    assertThat(repository.deleteOlderThan(Instant.parse("2026-01-01T00:00:00Z"))).isEqualTo(1);

    assertThat(repository.findDrilldown(query(0, 10)).items())
        .extracting(AiInvocationEvent::getSessionId)
        .containsExactly("kept");
  }

  private static AiInvocationEvent event(String sessionId, Instant occurredAt) {
    return AiInvocationEvent.succeeded(AiCapability.CHAT, "completion", Latency.ofMillis(10), OWNER)
        .sessionId(sessionId)
        .occurredAt(occurredAt)
        .build();
  }

  private static DrilldownQuery query(int page, int size) {
    return new DrilldownQuery(
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        page,
        size);
  }

  private String rawColumn(AiInvocationEvent event, String column) {
    return (String)
        em.getEntityManager()
            .createNativeQuery("SELECT " + column + " FROM ai_invocation_event WHERE id = ?")
            .setParameter(1, event.getId().getValue())
            .getSingleResult();
  }
}
