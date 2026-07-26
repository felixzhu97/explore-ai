package com.ai.metrics.infrastructure.persistence;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcAiInvocationEventRepository")
class JdbcAiInvocationEventRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcAiInvocationEventRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcAiInvocationEventRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("should_insert_event_with_all_columns")
    void should_insert_event_with_all_columns() {
        UUID id = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-26T10:00:00Z");
        AiInvocationEvent event = AiInvocationEvent.builder()
                .id(id)
                .occurredAt(occurredAt)
                .domain(AiDomain.CHAT)
                .operation("chat.stream")
                .outcome(InvocationOutcome.SUCCESS)
                .latencyMs(42)
                .provider("openai")
                .model("gpt-4")
                .sessionId("session-1")
                .documentId("doc-1")
                .agentType("researcher")
                .toolName("weather")
                .promptTokens(10)
                .completionTokens(20)
                .errorCode("E1")
                .errorMessage("failed")
                .build();

        repository.save(event);

        verify(jdbcTemplate).update(
                startsWith("INSERT INTO ai_invocation_events"),
                eq(id.toString()),
                eq(Timestamp.from(occurredAt)),
                eq("chat"),
                eq("chat.stream"),
                eq("success"),
                eq(42L),
                eq("openai"),
                eq("gpt-4"),
                eq("session-1"),
                eq("doc-1"),
                eq("researcher"),
                eq("weather"),
                eq(10),
                eq(20),
                eq("E1"),
                eq("failed"));
    }

    @Test
    @DisplayName("should_page_drilldown_with_all_filters_and_clamped_size")
    void should_page_drilldown_with_all_filters_and_clamped_size() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*)"), eq(Long.class), any(Object[].class)))
                .thenReturn(25L);
        when(jdbcTemplate.query(startsWith("SELECT id"), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<AiInvocationEvent> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("id")).thenReturn(UUID.randomUUID().toString());
                    when(rs.getTimestamp("occurred_at")).thenReturn(Timestamp.from(Instant.parse("2026-07-26T12:00:00Z")));
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
                    when(rs.getString("error_code")).thenReturn(null);
                    when(rs.getString("error_message")).thenReturn(null);
                    return List.of(mapper.mapRow(rs, 0));
                });

        AiInvocationEventRepository.DrilldownQuery query = new AiInvocationEventRepository.DrilldownQuery(
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

        ArgumentCaptor<Object[]> countArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForObject(
                startsWith("SELECT COUNT(*)"),
                eq(Long.class),
                countArgs.capture());
        assertThat(countArgs.getValue()).hasSizeGreaterThan(5);

        ArgumentCaptor<Object[]> pageArgs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(startsWith("SELECT id"), any(RowMapper.class), pageArgs.capture());
        Object[] args = pageArgs.getValue();
        assertThat(args[args.length - 2]).isEqualTo(100);
        assertThat(args[args.length - 1]).isEqualTo(0);
    }

    @Test
    @DisplayName("should_return_empty_page_when_total_count_is_null")
    void should_return_empty_page_when_total_count_is_null() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*)"), eq(Long.class), any(Object[].class)))
                .thenReturn(null);
        when(jdbcTemplate.query(startsWith("SELECT id"), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        AiInvocationEventRepository.PageResult page = repository.findDrilldown(
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
}
