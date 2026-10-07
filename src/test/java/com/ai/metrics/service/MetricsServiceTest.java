package com.ai.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.InvocationOutcome;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.model.ModuleStatus;
import com.ai.metrics.domain.repository.MetricsHealthGateway;
import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.repository.MetricsHealthGateway.McpHealth;
import com.ai.metrics.domain.repository.MetricsQueryRepository;
import com.ai.metrics.service.model.CapabilityInventory;
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
    assertThat(overview.capabilities().agents()).isEqualTo(new AgentsHealth(ModuleStatus.UP, 2, 2));
    assertThat(overview.capabilities().mcp()).isEqualTo(new McpHealth(ModuleStatus.UP, 3, 1));
    assertThat(overview.capabilities().system()).isEqualTo(ModuleStatus.UP);
  }

  @Test
  @DisplayName("should compute error rate when invocations exist")
  void shouldComputeErrorRateWhenInvocationsExist() {
    queryRepository.requestCount = 10;
    queryRepository.errorCount = 2;
    queryRepository.byCapability = List.of(new MetricsQueryRepository.NamedCount("chat", 8));

    MetricsOverview overview = useCase.getOverview("7d");

    assertThat(overview.requestCount()).isEqualTo(10);
    assertThat(overview.errorCount()).isEqualTo(2);
    assertThat(overview.errorRate()).isEqualTo(0.2);
    assertThat(overview.successRate()).isEqualTo(0.8);
    assertThat(overview.requestsByCapability()).extracting(NamedCount::name).contains("chat");
  }

  @Test
  @DisplayName("should filter drilldown by capability and day")
  void shouldFilterDrilldownByCapabilityAndDay() {
    eventRepository.events.add(
        AiInvocationEvent.createSucceededEvent(
                AiCapability.CHAT, "chat.stream", Latency.createFromMillis(12), OwnerKey.UNOWNED)
            .sessionId("s1")
            .build());

    DrilldownPage page =
        useCase.getDrilldown("chat", null, null, "2026-07-26", null, null, null, null, 0, 20, "7d");

    assertThat(page.total()).isEqualTo(1);
    assertThat(page.items()).hasSize(1);
    assertThat(eventRepository.lastQuery.getCapability()).contains(AiCapability.CHAT);
    assertThat(eventRepository.lastQuery.getDay()).contains("2026-07-26");
  }

  @Test
  @DisplayName("should reject unknown series name")
  void shouldRejectUnknownSeriesName() {
    assertThatThrownBy(() -> useCase.getSeries("unknown", "chat", "7d"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unknown series");
  }

  @Test
  @DisplayName("should return capability snapshot for each ai capability")
  void shouldReturnCapabilitySnapshotForEachAiCapability() {
    queryRepository.requestCount = 4;
    queryRepository.errorCount = 1;
    queryRepository.topTools = List.of(new MetricsQueryRepository.NamedCount("weather", 2));

    assertThat(useCase.getCapability("chat", "7d").inventory())
        .isInstanceOf(CapabilityInventory.Chat.class);
    assertThat(useCase.getCapability("rag", "7d").inventory())
        .isInstanceOf(CapabilityInventory.Rag.class);
    assertThat(useCase.getCapability("agents", "7d").inventory())
        .isEqualTo(new CapabilityInventory.Agents(new AgentsHealth(ModuleStatus.UP, 2, 2)));
    assertThat(useCase.getCapability("tools", "7d").inventory())
        .isEqualTo(new CapabilityInventory.Tools(List.of(new NamedCount("weather", 2))));
    assertThat(useCase.getCapability("vision", "30d").inventory())
        .isEqualTo(new CapabilityInventory.Requests(4, 1));
    assertThat(useCase.getCapability("vision", "30d").errorRate()).isEqualTo(0.25);
    assertThat(useCase.getCapability("workflow", "7d").requestCount()).isEqualTo(4);
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
    assertThat(eventRepository.lastQuery.getFrom()).isPresent();
    assertThat(eventRepository.lastQuery.getTo()).isPresent();
    assertThat(eventRepository.lastQuery.getOutcome()).contains(InvocationOutcome.SUCCESS);
    assertThat(eventRepository.lastQuery.getModel()).contains("gpt");
  }
}
