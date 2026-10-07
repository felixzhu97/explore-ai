package com.ai.metrics.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiDomain;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.model.TokenUsage;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import jakarta.persistence.EntityManager;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAiInvocationEventRepository")
class JpaAiInvocationEventRepositoryTest {

  @Mock private EntityManager entityManager;
  @Mock private JdbcTemplate jdbcTemplate;

  private JpaAiInvocationEventRepository repository;

  @BeforeEach
  void setUp() {
    repository = new JpaAiInvocationEventRepository(entityManager, jdbcTemplate);
  }

  @Test
  @DisplayName("should persist event with the owner fixed at creation")
  void shouldPersistEventWithTheOwnerFixedAtCreation() {
    UUID id = UUID.randomUUID();
    OwnerKey owner = OwnerKey.forClient("22222222-2222-4222-8222-222222222222");
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(AiDomain.CHAT, "chat.stream", Latency.ofMillis(42), owner)
            .id(id)
            .occurredAt(Instant.parse("2026-07-26T10:00:00Z"))
            .provider("openai")
            .model("gpt-4")
            .tokens(new TokenUsage(10, 20))
            .build();

    repository.save(event);

    ArgumentCaptor<AiInvocationEvent> saved = ArgumentCaptor.forClass(AiInvocationEvent.class);
    verify(entityManager).persist(saved.capture());
    verify(entityManager, never()).merge(any());
    verifyNoInteractions(jdbcTemplate);
    assertThat(saved.getValue().getOwnerKey()).isEqualTo(owner);
    assertThat(saved.getValue().getId().value()).isEqualTo(id.toString());
  }

  @Test
  @DisplayName("should page drilldown with all filters and clamped size")
  void shouldPageDrilldownWithAllFiltersAndClampedSize() {
    when(jdbcTemplate.queryForObject(
            startsWith("SELECT COUNT(*)"), eq(Long.class), any(Object[].class)))
        .thenReturn(25L);
    when(jdbcTemplate.query(startsWith("SELECT id"), any(RowMapper.class), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              final RowMapper<AiInvocationEvent> mapper = invocation.getArgument(1);
              ResultSet rs = mock(ResultSet.class);
              when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
              when(rs.getObject("occurred_at", Instant.class))
                  .thenReturn(Instant.parse("2026-07-26T12:00:00Z"));
              when(rs.getString("domain")).thenReturn("chat");
              when(rs.getString("operation")).thenReturn("chat.stream");
              when(rs.getString("outcome")).thenReturn("success");
              when(rs.getLong("latency_ms")).thenReturn(15L);
              when(rs.getString("provider")).thenReturn("openai");
              when(rs.getString("model")).thenReturn("gpt");
              when(rs.getString("session_id")).thenReturn("s1");
              when(rs.getString("document_id")).thenReturn(null);
              when(rs.getString("agent_type")).thenReturn(null);
              when(rs.getString("tool_name")).thenReturn(null);
              when(rs.getObject("prompt_tokens")).thenReturn(1);
              when(rs.getObject("completion_tokens")).thenReturn(2);
              when(rs.getString("owner_key")).thenReturn("c:33333333-3333-4333-8333-333333333333");
              return List.of(mapper.mapRow(rs, 0));
            });

    AiInvocationEventRepository.DrilldownQuery query =
        new AiInvocationEventRepository.DrilldownQuery(
            Optional.of(AiDomain.CHAT),
            Optional.of(Instant.parse("2026-07-01T00:00:00Z")),
            Optional.of(Instant.parse("2026-07-02T00:00:00Z")),
            Optional.of("2026-07-26"),
            Optional.of(InvocationOutcome.ERROR),
            Optional.of("gpt"),
            Optional.of("researcher"),
            Optional.of("weather"),
            -2,
            500);

    AiInvocationEventRepository.PageResult page = repository.findDrilldown(query);

    assertThat(page.total()).isEqualTo(25L);
    assertThat(page.items()).hasSize(1);
    assertThat(page.items().getFirst().getDomain()).isEqualTo(AiDomain.CHAT);
    assertThat(page.items().getFirst().getOwnerKey().value())
        .isEqualTo("c:33333333-3333-4333-8333-333333333333");

    ArgumentCaptor<Object[]> countArgs = ArgumentCaptor.forClass(Object[].class);
    verify(jdbcTemplate)
        .queryForObject(startsWith("SELECT COUNT(*)"), eq(Long.class), countArgs.capture());
    assertThat(countArgs.getValue()).hasSizeGreaterThan(5);

    ArgumentCaptor<Object[]> pageArgs = ArgumentCaptor.forClass(Object[].class);
    verify(jdbcTemplate).query(startsWith("SELECT id"), any(RowMapper.class), pageArgs.capture());
    Object[] args = pageArgs.getValue();
    assertThat(args[args.length - 2]).isEqualTo(100);
    assertThat(args[args.length - 1]).isEqualTo(0);
  }

  @Test
  @DisplayName("should return empty page when total count is null")
  void shouldReturnEmptyPageWhenTotalCountIsNull() {
    when(jdbcTemplate.queryForObject(
            startsWith("SELECT COUNT(*)"), eq(Long.class), any(Object[].class)))
        .thenReturn(null);
    when(jdbcTemplate.query(startsWith("SELECT id"), any(RowMapper.class), any(Object[].class)))
        .thenReturn(List.of());

    AiInvocationEventRepository.PageResult page =
        repository.findDrilldown(
            new AiInvocationEventRepository.DrilldownQuery(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                1,
                10));

    assertThat(page.total()).isZero();
    assertThat(page.items()).isEmpty();
  }

  @Test
  @DisplayName("should delete events by session ids")
  void shouldDeleteEventsWhenSessionIdsProvided() {
    when(jdbcTemplate.update(anyString(), eq("s1"), eq("s2"))).thenReturn(2);

    int deleted = repository.deleteBySessionIds(List.of("s1", "s2"));

    assertThat(deleted).isEqualTo(2);
  }

  @Test
  @DisplayName("should delete events older than cutoff")
  void shouldDeleteEventsWhenOlderThanCutoff() {
    Instant cutoff = Instant.parse("2026-01-01T00:00:00Z");
    when(jdbcTemplate.update(anyString(), eq(cutoff))).thenReturn(5);

    int deleted = repository.deleteOlderThan(cutoff);

    assertThat(deleted).isEqualTo(5);
  }
}
