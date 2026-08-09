package com.ai.metrics.infrastructure.persistence;

import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcMetricsQueryRepository")
class JdbcMetricsQueryRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcMetricsQueryRepository repository;

    private final Instant from = Instant.parse("2026-07-01T00:00:00Z");
    private final Instant to = Instant.parse("2026-07-08T00:00:00Z");

    @BeforeEach
    void setUp() {
        repository = new JdbcMetricsQueryRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("should count invocations with optional domain filter")
    void shouldCountInvocationsWithOptionalDomainFilter() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(7L);

        long count = repository.countInvocations(Optional.of(AiDomain.CHAT), from, to);

        assertThat(count).isEqualTo(7L);
        verify(jdbcTemplate).queryForObject(
                contains("SELECT COUNT(*) FROM ai_invocation_events"),
                eq(Long.class),
                any(Object[].class));
    }

    @Test
    @DisplayName("should count errors and treat null as zero")
    void shouldCountErrorsAndTreatNullAsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(null);

        long count = repository.countErrors(Optional.empty(), from, to);

        assertThat(count).isZero();
        verify(jdbcTemplate).queryForObject(contains("outcome = 'error'"), eq(Long.class), any(Object[].class));
    }

    @Test
    @DisplayName("should return null percentiles when no latency rows")
    void shouldReturnNullPercentilesWhenNoLatencyRows() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        MetricsQueryRepository.LatencyStats stats =
                repository.latencyPercentiles(Optional.of(AiDomain.RAG), from, to);

        assertThat(stats.p50Ms()).isNull();
        assertThat(stats.p95Ms()).isNull();
    }

    @Test
    @DisplayName("should compute latency percentiles from sorted rows")
    void shouldComputeLatencyPercentilesFromSortedRows() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<Long> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("latency_ms")).thenReturn(10L, 20L, 30L, 40L, 100L);
                    List<Long> values = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        values.add(mapper.mapRow(rs, i));
                    }
                    return values;
                });

        MetricsQueryRepository.LatencyStats stats =
                repository.latencyPercentiles(Optional.empty(), from, to);

        assertThat(stats.p50Ms()).isEqualTo(30.0);
        assertThat(stats.p95Ms()).isEqualTo(88.0);
    }

    @Test
    @DisplayName("should sum token totals from result set")
    void shouldSumTokenTotalsFromResultSet() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<MetricsQueryRepository.TokenTotals> extractor = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getLong("prompt_tokens")).thenReturn(120L);
            when(rs.getLong("completion_tokens")).thenReturn(45L);
            return extractor.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));

        MetricsQueryRepository.TokenTotals totals =
                repository.tokenTotals(Optional.of(AiDomain.CHAT), from, to);

        assertThat(totals.promptTokens()).isEqualTo(120L);
        assertThat(totals.completionTokens()).isEqualTo(45L);
    }

    @Test
    @DisplayName("should return zero tokens when result set empty")
    void shouldReturnZeroTokensWhenResultSetEmpty() {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<MetricsQueryRepository.TokenTotals> extractor = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            return extractor.extractData(rs);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class), any(Object[].class));

        MetricsQueryRepository.TokenTotals totals = repository.tokenTotals(Optional.empty(), from, to);

        assertThat(totals.promptTokens()).isZero();
        assertThat(totals.completionTokens()).isZero();
    }

    @Test
    @DisplayName("should map named counts for domain model and agent queries")
    void shouldMapNamedCountsForDomainModelAndAgentQueries() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<MetricsQueryRepository.NamedCount> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("name")).thenReturn("chat");
                    when(rs.getLong("cnt")).thenReturn(5L);
                    return List.of(mapper.mapRow(rs, 0));
                });

        assertThat(repository.countByDomain(from, to)).extracting(MetricsQueryRepository.NamedCount::name)
                .containsExactly("chat");
        assertThat(repository.countByModel(Optional.of(AiDomain.CHAT), from, to)).hasSize(1);
        assertThat(repository.countByAgentType(from, to)).hasSize(1);
    }

    @Test
    @DisplayName("should apply minimum limit when querying top tools")
    void shouldApplyMinimumLimitWhenQueryingTopTools() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

        repository.topTools(Optional.of(AiDomain.TOOLS), from, to, 0);

        verify(jdbcTemplate).query(contains("LIMIT ?"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @DisplayName("should return daily time points from events and related tables")
    void shouldReturnDailyTimePointsFromEventsAndRelatedTables() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<MetricsQueryRepository.TimePoint> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("bucket_day")).thenReturn("2026-07-01");
                    when(rs.getLong("metric_value")).thenReturn(3L);
                    return List.of(mapper.mapRow(rs, 0));
                });

        assertThat(repository.dailyRequests(Optional.of(AiDomain.CHAT), from, to)).hasSize(1);
        assertThat(repository.dailyErrors(Optional.empty(), from, to)).hasSize(1);
        assertThat(repository.dailySessionsCreated(from, to)).hasSize(1);
        assertThat(repository.dailyMessagesCreated(from, to)).hasSize(1);
        assertThat(repository.dailyDocumentsUploaded(from, to)).hasSize(1);
    }

    @Test
    @DisplayName("should compute daily p95 latency per bucket day")
    void shouldComputeDailyP95LatencyPerBucketDay() {
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("bucket_day")).thenReturn("2026-07-01", "2026-07-01", "2026-07-02");
            when(rs.getLong("latency_ms")).thenReturn(10L, 90L, 50L);
            handler.processRow(rs);
            handler.processRow(rs);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));

        List<MetricsQueryRepository.TimePoint> points =
                repository.dailyLatencyP95(Optional.of(AiDomain.AGENTS), from, to);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).day()).isEqualTo("2026-07-01");
        assertThat(points.get(0).value()).isEqualTo(86L);
        assertThat(points.get(1).day()).isEqualTo("2026-07-02");
        assertThat(points.get(1).value()).isEqualTo(50L);
    }

    @Test
    @DisplayName("should build chat inventory with null safe counts")
    void shouldBuildChatInventoryWithNullSafeCounts() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM chat_sessions"), eq(Long.class)))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM chat_sessions WHERE last_activity_at >= ?"),
                eq(Long.class),
                any()))
                .thenReturn(2L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM SPRING_AI_CHAT_MEMORY"), eq(Long.class)))
                .thenReturn(10L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM chat_web_sources"), eq(Long.class)))
                .thenReturn(1L);

        MetricsQueryRepository.ChatInventory inventory = repository.chatInventory(from);

        assertThat(inventory.sessionCount()).isZero();
        assertThat(inventory.activeSessionCount()).isEqualTo(2L);
        assertThat(inventory.messageCount()).isEqualTo(10L);
        assertThat(inventory.webSourceReplyCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should build rag inventory with status breakdown")
    void shouldBuildRagInventoryWithStatusBreakdown() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM documents"), eq(Long.class))).thenReturn(4L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM document_chunks"), eq(Long.class))).thenReturn(12L);
        when(jdbcTemplate.queryForObject(eq("SELECT COALESCE(SUM(file_size), 0) FROM documents"), eq(Long.class)))
                .thenReturn(2048L);
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet rs = mock(ResultSet.class);
            when(rs.getString("status")).thenReturn("READY");
            when(rs.getLong("cnt")).thenReturn(3L);
            handler.processRow(rs);
            return null;
        }).when(jdbcTemplate).query(
                eq("SELECT status, COUNT(*) AS cnt FROM documents GROUP BY status"),
                any(RowCallbackHandler.class));

        MetricsQueryRepository.RagInventory inventory = repository.ragInventory();

        assertThat(inventory.documentCount()).isEqualTo(4L);
        assertThat(inventory.chunkCount()).isEqualTo(12L);
        assertThat(inventory.totalFileBytes()).isEqualTo(2048L);
        assertThat(inventory.documentsByStatus()).containsEntry("READY", 3L);
    }
}
