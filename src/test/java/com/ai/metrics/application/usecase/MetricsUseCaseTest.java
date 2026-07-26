package com.ai.metrics.application.usecase;

import com.ai.metrics.application.model.DrilldownPage;
import com.ai.metrics.application.model.MetricsOverview;
import com.ai.metrics.application.model.NamedCount;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MetricsUseCase")
class MetricsUseCaseTest {

    private FakeQueryRepository queryRepository;
    private FakeEventRepository eventRepository;
    private MetricsUseCase useCase;

    @BeforeEach
    void setUp() {
        queryRepository = new FakeQueryRepository();
        eventRepository = new FakeEventRepository();
        MetricsHealthGateway healthGateway = new MetricsHealthGateway() {
            @Override
            public Map<String, Object> systemStatus() {
                return Map.of("status", "UP");
            }

            @Override
            public Map<String, Object> agentsHealth() {
                return Map.of("status", "UP", "agentCount", 2, "healthyAgentCount", 2);
            }

            @Override
            public Map<String, Object> mcpHealth() {
                return Map.of("status", "UP", "registeredTools", 3, "connectedServers", 1);
            }
        };
        useCase = new MetricsUseCase(queryRepository, eventRepository, healthGateway);
    }

    @Test
    @DisplayName("should_return_zero_rates_when_no_invocations")
    void should_return_zero_rates_when_no_invocations() {
        MetricsOverview overview = useCase.overview("7d");

        assertThat(overview.requestCount()).isZero();
        assertThat(overview.errorCount()).isZero();
        assertThat(overview.errorRate()).isZero();
        assertThat(overview.successRate()).isEqualTo(1.0);
        assertThat(overview.domains()).containsKeys("chat", "rag", "agents", "mcp", "system");
    }

    @Test
    @DisplayName("should_compute_error_rate_when_invocations_exist")
    void should_compute_error_rate_when_invocations_exist() {
        queryRepository.requestCount = 10;
        queryRepository.errorCount = 2;
        queryRepository.byDomain = List.of(new MetricsQueryRepository.NamedCount("chat", 8));

        MetricsOverview overview = useCase.overview("7d");

        assertThat(overview.requestCount()).isEqualTo(10);
        assertThat(overview.errorCount()).isEqualTo(2);
        assertThat(overview.errorRate()).isEqualTo(0.2);
        assertThat(overview.successRate()).isEqualTo(0.8);
        assertThat(overview.requestsByDomain()).extracting(NamedCount::name).contains("chat");
    }

    @Test
    @DisplayName("should_filter_drilldown_by_domain_and_day")
    void should_filter_drilldown_by_domain_and_day() {
        eventRepository.events.add(AiInvocationEvent.builder()
                .domain(AiDomain.CHAT)
                .operation("chat.stream")
                .outcome(InvocationOutcome.SUCCESS)
                .latencyMs(12)
                .sessionId("s1")
                .build());

        DrilldownPage page = useCase.drilldown("chat", null, null, "2026-07-26", null, null, null, null, 0, 20, "7d");

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).hasSize(1);
        assertThat(eventRepository.lastQuery.domain()).contains(AiDomain.CHAT);
        assertThat(eventRepository.lastQuery.day()).contains("2026-07-26");
    }

    @Test
    @DisplayName("should_reject_unknown_series_name")
    void should_reject_unknown_series_name() {
        assertThatThrownBy(() -> useCase.series("unknown", "chat", "7d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown series");
    }

    @Test
    @DisplayName("should_return_domain_snapshot_for_each_ai_domain")
    void should_return_domain_snapshot_for_each_ai_domain() {
        queryRepository.requestCount = 4;
        queryRepository.errorCount = 1;
        queryRepository.topTools = List.of(new MetricsQueryRepository.NamedCount("weather", 2));

        assertThat(useCase.domain("chat", "7d").inventory()).containsKey("sessionCount");
        assertThat(useCase.domain("rag", "7d").inventory()).containsKey("documentCount");
        assertThat(useCase.domain("agents", "7d").inventory()).containsKey("agentCount");
        assertThat(useCase.domain("tools", "7d").inventory()).containsKey("topTools");
        assertThat(useCase.domain("vision", "30d").errorRate()).isEqualTo(0.25);
        assertThat(useCase.domain("workflow", "7d").requestCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("should_return_named_series_when_known")
    void should_return_named_series_when_known() {
        queryRepository.dailyRequests = List.of(new MetricsQueryRepository.TimePoint("2026-07-01", 3));
        queryRepository.dailyErrors = List.of(new MetricsQueryRepository.TimePoint("2026-07-01", 1));
        queryRepository.dailyLatency = List.of(new MetricsQueryRepository.TimePoint("2026-07-01", 40));
        queryRepository.byModel = List.of(new MetricsQueryRepository.NamedCount("gpt", 2));
        queryRepository.byAgent = List.of(new MetricsQueryRepository.NamedCount("researcher", 1));
        queryRepository.topTools = List.of(new MetricsQueryRepository.NamedCount("weather", 2));
        queryRepository.documentsByStatus.put("READY", 5L);

        assertThat(useCase.series("requests", "chat", "7d").points()).hasSize(1);
        assertThat(useCase.series("errors", null, "7d").points()).hasSize(1);
        assertThat(useCase.series("latency_p95", "chat", "7d").points()).hasSize(1);
        assertThat(useCase.series("sessions_created", null, "7d").points()).isEmpty();
        assertThat(useCase.series("messages_created", null, "7d").points()).isEmpty();
        assertThat(useCase.series("documents_uploaded", null, "7d").points()).isEmpty();
        assertThat(useCase.series("documents_by_status", null, "7d").points())
                .extracting(p -> p.label())
                .contains("READY");
        assertThat(useCase.series("calls_by_model", "chat", "7d").points()).hasSize(1);
        assertThat(useCase.series("calls_by_agent", null, "7d").points()).hasSize(1);
        assertThat(useCase.series("tool_top", "tools", "7d").points()).hasSize(1);
        assertThat(useCase.series("tokens", "chat", "7d").points()).hasSize(2);
        assertThat(useCase.series("  REQUESTS ", "chat", "7d").name()).isEqualTo("requests");
    }

    @Test
    @DisplayName("should_reject_unsupported_range_when_parsing")
    void should_reject_unsupported_range_when_parsing() {
        assertThatThrownBy(() -> useCase.overview("90d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported range");
    }

    @Test
    @DisplayName("should_default_page_size_and_use_explicit_window_when_provided")
    void should_default_page_size_and_use_explicit_window_when_provided() {
        DrilldownPage page = useCase.drilldown(
                "chat",
                "2026-07-01T00:00:00Z",
                "2026-07-02T00:00:00Z",
                null,
                "success",
                "gpt",
                null,
                null,
                -1,
                0,
                null);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(eventRepository.lastQuery.from()).isPresent();
        assertThat(eventRepository.lastQuery.to()).isPresent();
        assertThat(eventRepository.lastQuery.outcome()).contains(InvocationOutcome.SUCCESS);
        assertThat(eventRepository.lastQuery.model()).contains("gpt");
    }

    private static final class FakeQueryRepository implements MetricsQueryRepository {
        long requestCount;
        long errorCount;
        List<NamedCount> byDomain = List.of();
        List<NamedCount> topTools = List.of();
        List<NamedCount> byModel = List.of();
        List<NamedCount> byAgent = List.of();
        List<TimePoint> dailyRequests = List.of();
        List<TimePoint> dailyErrors = List.of();
        List<TimePoint> dailyLatency = List.of();
        final LinkedHashMap<String, Long> documentsByStatus = new LinkedHashMap<>();

        @Override
        public long countInvocations(Optional<AiDomain> domain, Instant from, Instant to) {
            return requestCount;
        }

        @Override
        public long countErrors(Optional<AiDomain> domain, Instant from, Instant to) {
            return errorCount;
        }

        @Override
        public LatencyStats latencyPercentiles(Optional<AiDomain> domain, Instant from, Instant to) {
            return new LatencyStats(10.0, 40.0);
        }

        @Override
        public TokenTotals tokenTotals(Optional<AiDomain> domain, Instant from, Instant to) {
            return new TokenTotals(11L, 22L);
        }

        @Override
        public List<NamedCount> countByDomain(Instant from, Instant to) {
            return byDomain;
        }

        @Override
        public List<NamedCount> countByModel(Optional<AiDomain> domain, Instant from, Instant to) {
            return byModel;
        }

        @Override
        public List<NamedCount> countByAgentType(Instant from, Instant to) {
            return byAgent;
        }

        @Override
        public List<NamedCount> topTools(Optional<AiDomain> domain, Instant from, Instant to, int limit) {
            return topTools;
        }

        @Override
        public List<TimePoint> dailyRequests(Optional<AiDomain> domain, Instant from, Instant to) {
            return dailyRequests;
        }

        @Override
        public List<TimePoint> dailyErrors(Optional<AiDomain> domain, Instant from, Instant to) {
            return dailyErrors;
        }

        @Override
        public List<TimePoint> dailyLatencyP95(Optional<AiDomain> domain, Instant from, Instant to) {
            return dailyLatency;
        }

        @Override
        public List<TimePoint> dailySessionsCreated(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<TimePoint> dailyMessagesCreated(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<TimePoint> dailyDocumentsUploaded(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public ChatInventory chatInventory(Instant activeSince) {
            return new ChatInventory(0, 0, 0, 0);
        }

        @Override
        public RagInventory ragInventory() {
            return new RagInventory(0, documentsByStatus, 0, 0);
        }
    }

    private static final class FakeEventRepository implements AiInvocationEventRepository {
        final List<AiInvocationEvent> events = new ArrayList<>();
        DrilldownQuery lastQuery;

        @Override
        public void save(AiInvocationEvent event) {
            events.add(event);
        }

        @Override
        public PageResult findDrilldown(DrilldownQuery query) {
            lastQuery = query;
            return new PageResult(events, events.size());
        }
    }
}
