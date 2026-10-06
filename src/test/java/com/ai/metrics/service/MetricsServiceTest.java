package com.ai.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.repository.MetricsHealthGateway.McpHealth;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.domain.vo.Latency;
import com.ai.metrics.domain.vo.ModuleStatus;
import com.ai.metrics.service.model.DomainInventory;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.NamedCount;
import com.ai.metrics.test.fixture.FakeAiInvocationEventRepository;
import com.ai.metrics.test.fixture.FakeMetricsQueryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MetricsService")
class MetricsServiceTest {

  private FakeMetricsQueryRepository queryRepository;
  private FakeAiInvocationEventRepository eventRepository;
  private MetricsService useCase;

  @BeforeEach
  void setUp() {
    queryRepository = new FakeMetricsQueryRepository();
    eventRepository = new FakeAiInvocationEventRepository();
    MetricsHealthGateway healthGateway =
        new MetricsHealthGateway() {
          @Override
          public ModuleStatus getSystemStatus() {
            return ModuleStatus.UP;
          }

          @Override
          public AgentsHealth checkAgentsHealth() {
            return new AgentsHealth(ModuleStatus.UP, 2, 2);
          }

          @Override
          public McpHealth checkMcpHealth() {
            return new McpHealth(ModuleStatus.UP, 3, 1);
          }
        };
    useCase = new MetricsService(queryRepository, eventRepository, healthGateway);
  }

  @Test
  @DisplayName("should return zero rates when no invocations")
  void shouldReturnZeroRatesWhenNoInvocations() {
    MetricsOverview overview = useCase.getOverview("7d");

    assertThat(overview.requestCount()).isZero();
    assertThat(overview.errorCount()).isZero();
    assertThat(overview.errorRate()).isZero();
    assertThat(overview.successRate()).isEqualTo(1.0);
    assertThat(overview.domains().agents()).isEqualTo(new AgentsHealth(ModuleStatus.UP, 2, 2));
    assertThat(overview.domains().mcp()).isEqualTo(new McpHealth(ModuleStatus.UP, 3, 1));
    assertThat(overview.domains().system()).isEqualTo(ModuleStatus.UP);
  }

  @Test
  @DisplayName("should compute error rate when invocations exist")
  void shouldComputeErrorRateWhenInvocationsExist() {
    queryRepository.requestCount = 10;
    queryRepository.errorCount = 2;
    queryRepository.byDomain = List.of(new MetricsQueryRepository.NamedCount("chat", 8));

    MetricsOverview overview = useCase.getOverview("7d");

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
        AiInvocationEvent.succeeded(
                AiDomain.CHAT, "chat.stream", Latency.ofMillis(12), OwnerKey.UNOWNED)
            .sessionId("s1")
            .build());

    DrilldownPage page =
        useCase.getDrilldown("chat", null, null, "2026-07-26", null, null, null, null, 0, 20, "7d");

    assertThat(page.total()).isEqualTo(1);
    assertThat(page.items()).hasSize(1);
    assertThat(eventRepository.lastQuery.domain()).contains(AiDomain.CHAT);
    assertThat(eventRepository.lastQuery.day()).contains("2026-07-26");
  }

  @Test
  @DisplayName("should reject unknown series name")
  void shouldRejectUnknownSeriesName() {
    assertThatThrownBy(() -> useCase.getSeries("unknown", "chat", "7d"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown series");
  }

  @Test
  @DisplayName("should return domain snapshot for each ai domain")
  void shouldReturnDomainSnapshotForEachAiDomain() {
    queryRepository.requestCount = 4;
    queryRepository.errorCount = 1;
    queryRepository.topTools = List.of(new MetricsQueryRepository.NamedCount("weather", 2));

    assertThat(useCase.getDomain("chat", "7d").inventory())
        .isInstanceOf(DomainInventory.Chat.class);
    assertThat(useCase.getDomain("rag", "7d").inventory()).isInstanceOf(DomainInventory.Rag.class);
    assertThat(useCase.getDomain("agents", "7d").inventory())
        .isEqualTo(new DomainInventory.Agents(new AgentsHealth(ModuleStatus.UP, 2, 2)));
    assertThat(useCase.getDomain("tools", "7d").inventory())
        .isEqualTo(new DomainInventory.Tools(List.of(new NamedCount("weather", 2))));
    assertThat(useCase.getDomain("vision", "30d").inventory())
        .isEqualTo(new DomainInventory.Requests(4, 1));
    assertThat(useCase.getDomain("vision", "30d").errorRate()).isEqualTo(0.25);
    assertThat(useCase.getDomain("workflow", "7d").requestCount()).isEqualTo(4);
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

    assertThat(useCase.getSeries("requests", "chat", "7d").points()).hasSize(1);
    assertThat(useCase.getSeries("errors", null, "7d").points()).hasSize(1);
    assertThat(useCase.getSeries("latency_p95", "chat", "7d").points()).hasSize(1);
    assertThat(useCase.getSeries("sessions_created", null, "7d").points()).isEmpty();
    assertThat(useCase.getSeries("messages_created", null, "7d").points()).isEmpty();
    assertThat(useCase.getSeries("documents_uploaded", null, "7d").points()).isEmpty();
    assertThat(useCase.getSeries("documents_by_status", null, "7d").points())
        .extracting(p -> p.label())
        .contains("READY");
    assertThat(useCase.getSeries("calls_by_model", "chat", "7d").points()).hasSize(1);
    assertThat(useCase.getSeries("calls_by_agent", null, "7d").points()).hasSize(1);
    assertThat(useCase.getSeries("tool_top", "tools", "7d").points()).hasSize(1);
    assertThat(useCase.getSeries("tokens", "chat", "7d").points()).hasSize(2);
    assertThat(useCase.getSeries("  REQUESTS ", "chat", "7d").name()).isEqualTo("requests");
  }

  @Test
  @DisplayName("should reject unsupported range when parsing")
  void shouldRejectUnsupportedRangeWhenParsing() {
    assertThatThrownBy(() -> useCase.getOverview("90d"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported range");
  }

  @Test
  @DisplayName("should default page size and use explicit window when provided")
  void shouldDefaultPageSizeAndUseExplicitWindowWhenProvided() {
    DrilldownPage page =
        useCase.getDrilldown(
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
