package com.ai.metrics.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiDomain;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.ErrorSummary;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.repository.AiInvocationEventRepository.DrilldownQuery;
import com.ai.metrics.domain.repository.AiInvocationEventRepository.PageResult;
import com.ai.testsupport.AbstractDataJpaTest;
import java.time.Instant;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class AiInvocationEventJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_KEY = "c:77777777-7777-7777-7777-777777777777";
  private static final OwnerKey OWNER = OwnerKey.parse(OWNER_KEY);

  @Autowired private DataSource dataSource;

  @Test
  @DisplayName("should persist and reload invocation event when round tripping")
  void shouldPersistAndReloadInvocationEventWhenRoundTripping() {
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(AiDomain.CHAT, "completion", Latency.ofMillis(250), OWNER)
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
  @DisplayName("should store domain and outcome values when persisting event")
  void shouldStoreDomainAndOutcomeValuesWhenPersistingEvent() {
    AiInvocationEvent event =
        AiInvocationEvent.failed(
                AiDomain.RAG,
                "embed",
                Latency.ofMillis(90),
                OWNER,
                ErrorSummary.of("timeout", null))
            .build();

    em.persistAndFlush(event);
    em.clear();

    AiInvocationEvent reloaded = em.find(AiInvocationEvent.class, event.getId());

    assertThat(reloaded.getDomain()).isEqualTo(AiDomain.RAG);
    assertThat(reloaded.getOutcome()).isEqualTo(InvocationOutcome.ERROR);
    assertThat(reloaded.getErrorCode()).isEqualTo("timeout");
  }

  @Test
  @DisplayName("should store lowercase domain and outcome when persisting event")
  void shouldStoreLowercaseDomainAndOutcomeWhenPersistingEvent() {
    AiInvocationEvent event =
        AiInvocationEvent.failed(
                AiDomain.VISION,
                "describe",
                Latency.ofMillis(5),
                OWNER,
                ErrorSummary.of("unknown", null))
            .build();

    em.persistAndFlush(event);
    em.clear();

    assertThat(rawColumn(event, "domain")).isEqualTo("vision");
    assertThat(rawColumn(event, "outcome")).isEqualTo("error");
  }

  @Test
  @DisplayName("should preserve occurred at timestamp when round tripping event")
  void shouldPreserveOccurredAtTimestampWhenRoundTrippingEvent() {
    Instant occurredAt = Instant.parse("2026-03-15T12:00:00Z");
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(AiDomain.WORKFLOW, "execute", Latency.ofMillis(500), OWNER)
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
        AiInvocationEvent.succeeded(AiDomain.WORKFLOW, "execute", Latency.ofMillis(500), OWNER)
            .occurredAt(occurredAt)
            .build();
    em.persistAndFlush(event);
    em.clear();
    JpaAiInvocationEventRepository repository =
        new JpaAiInvocationEventRepository(em.getEntityManager(), new JdbcTemplate(dataSource));

    PageResult page =
        repository.findDrilldown(
            new DrilldownQuery(
                Optional.of(AiDomain.WORKFLOW),
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

  private String rawColumn(AiInvocationEvent event, String column) {
    return (String)
        em.getEntityManager()
            .createNativeQuery("SELECT " + column + " FROM ai_invocation_event WHERE id = ?")
            .setParameter(1, event.getId().getValue())
            .getSingleResult();
  }
}
