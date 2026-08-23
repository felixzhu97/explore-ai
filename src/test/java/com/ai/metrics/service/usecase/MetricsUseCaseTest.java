package com.ai.metrics.service.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.NamedCount;
import com.ai.metrics.test.fixture.FakeAiInvocationEventRepository;
import com.ai.metrics.test.fixture.FakeMetricsQueryRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MetricsUseCase")
class MetricsUseCaseTest {

  private FakeMetricsQueryRepository queryRepository;
  private FakeAiInvocationEventRepository eventRepository;
  private MetricsUseCase useCase;

  @BeforeEach
  void setUp() {
    queryRepository = new FakeMetricsQueryRepository();
    eventRepository = new FakeAiInvocationEventRepository();
    MetricsHealthGateway healthGateway =
        new MetricsHealthGateway() {
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
  @DisplayName("should return zero rates when no invocations")
  void shouldReturnZeroRatesWhenNoInvocations() {
    MetricsOverview overview = useCase.overview("7d");

    assertThat(overview.requestCount()).isZero();
    assertThat(overview.errorCount()).isZero();
    assertThat(overview.errorRate()).isZero();
    assertThat(overview.successRate()).isEqualTo(1.0);
    assertThat(overview.domains()).containsKeys("chat", "rag", "agents", "mcp", "system");
  }

  @Test
  @DisplayName("should compute error rate when invocations exist")
  void shouldComputeErrorRateWhenInvocationsExist() {
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
  @DisplayName("should filter drilldown by domain and day")
  void shouldFilterDrilldownByDomainAndDay() {
    eventRepository.events.add(
        AiInvocationEvent.builder()
            .domain(AiDomain.CHAT)
            .operation("chat.stream")
            .outcome(InvocationOutcome.SUCCESS)
            .latencyMs(12)
            .sessionId("s1")
            .build());

    DrilldownPage page =
        useCase.drilldown("chat", null, null, "2026-07-26", null, null, null, null, 0, 20, "7d");

    assertThat(page.total()).isEqualTo(1);
    assertThat(page.items()).hasSize(1);
    assertThat(eventRepository.lastQuery.domain()).contains(AiDomain.CHAT);
    assertThat(eventRepository.lastQuery.day()).contains("2026-07-26");
  }

  @Test
  @DisplayName("should reject unknown series name")
  void shouldRejectUnknownSeriesName() {
    assertThatThrownBy(() -> useCase.series("unknown", "chat", "7d"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown series");
  }

  @Test
  @DisplayName("should return domain snapshot for each ai domain")
  void shouldReturnDomainSnapshotForEachAiDomain() {
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
  @DisplayName("should return named series when known")
  void shouldReturnNamedSeriesWhenKnown() {
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
  @DisplayName("should reject unsupported range when parsing")
  void shouldRejectUnsupportedRangeWhenParsing() {
    assertThatThrownBy(() -> useCase.overview("90d"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported range");
  }

  @Test
  @DisplayName("should default page size and use explicit window when provided")
  void shouldDefaultPageSizeAndUseExplicitWindowWhenProvided() {
    DrilldownPage page =
        useCase.drilldown(
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
}
