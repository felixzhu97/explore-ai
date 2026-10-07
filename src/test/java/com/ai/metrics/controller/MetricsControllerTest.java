package com.ai.metrics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.metrics.domain.model.AiDomain;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.domain.model.ModuleStatus;
import com.ai.metrics.domain.model.TokenUsage;
import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.repository.MetricsHealthGateway.McpHealth;
import com.ai.metrics.domain.repository.MetricsQueryRepository.ChatInventory;
import com.ai.metrics.domain.repository.MetricsQueryRepository.RagInventory;
import com.ai.metrics.service.MetricsService;
import com.ai.metrics.service.model.DomainInventory;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsDomainSnapshot;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.NamedCount;
import com.ai.metrics.service.model.OverviewDomains;
import com.ai.metrics.service.model.SeriesPoint;
import com.ai.metrics.service.model.SeriesSnapshot;
import com.ai.testsupport.SliceWebMvcTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = MetricsController.class)
@DisplayName("MetricsController")
class MetricsControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private MetricsService metricsService;

  @Test
  @DisplayName("should map overview response from use case")
  void shouldMapOverviewResponseFromService() {
    MetricsOverview overview =
        new MetricsOverview(
            "7d",
            100,
            5,
            0.95,
            0.05,
            10.0,
            40.0,
            1000L,
            500L,
            List.of(new NamedCount("chat", 80)),
            new OverviewDomains(
                new ChatInventory(3, 1, 10, 0),
                new RagInventory(2, Map.of("READY", 2L), 8, 1024),
                new AgentsHealth(ModuleStatus.UP, 2, 2),
                new McpHealth(ModuleStatus.DISABLED, 0, 0),
                ModuleStatus.UP));
    when(metricsService.getOverview("7d")).thenReturn(overview);

    var result = mvc.get().uri("/api/metrics/overview").param("range", "7d").exchange();

    assertThat(result).hasStatusOk();
    assertThat(result).bodyJson().extractingPath("$.range").asString().isEqualTo("7d");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.requestCount")
        .convertTo(Integer.class)
        .isEqualTo(100);
    assertThat(result)
        .bodyJson()
        .extractingPath("$.requestsByDomain[0].name")
        .asString()
        .isEqualTo("chat");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.domains.mcp.status")
        .asString()
        .isEqualTo("DISABLED");
    assertThat(result)
        .bodyJson()
        .extractingPath("$.domains.system.status")
        .asString()
        .isEqualTo("UP");
    verify(metricsService).getOverview("7d");
  }

  @Test
  @DisplayName("should map domain response from use case")
  void shouldMapDomainResponseFromService() {
    MetricsDomainSnapshot snapshot =
        new MetricsDomainSnapshot(
            "chat",
            "7d",
            20,
            2,
            0.1,
            12.0,
            35.0,
            100L,
            50L,
            new DomainInventory.Chat(new ChatInventory(3, 1, 10, 0)),
            List.of(new SeriesPoint("2026-07-01", 5)),
            List.of(new SeriesPoint("gpt", 4)));
    when(metricsService.getDomain("chat", "7d")).thenReturn(snapshot);

    assertThat(mvc.get().uri("/api/metrics/domains/chat").param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.domain")
        .asString()
        .isEqualTo("chat");

    assertThat(mvc.get().uri("/api/metrics/domains/chat").param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.inventory.sessionCount")
        .convertTo(Integer.class)
        .isEqualTo(3);

    assertThat(mvc.get().uri("/api/metrics/domains/chat").param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.modelSeries[0].label")
        .asString()
        .isEqualTo("gpt");
  }

  @Test
  @DisplayName("should map series response from use case")
  void shouldMapSeriesResponseFromService() {
    SeriesSnapshot snapshot =
        new SeriesSnapshot("requests", "chat", "7d", List.of(new SeriesPoint("2026-07-01", 9)));
    when(metricsService.getSeries("requests", "chat", "7d")).thenReturn(snapshot);

    assertThat(
            mvc.get()
                .uri("/api/metrics/series")
                .param("name", "requests")
                .param("domain", "chat")
                .param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.name")
        .asString()
        .isEqualTo("requests");

    assertThat(
            mvc.get()
                .uri("/api/metrics/series")
                .param("name", "requests")
                .param("domain", "chat")
                .param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.points[0].label")
        .asString()
        .isEqualTo("2026-07-01");
  }

  @Test
  @DisplayName("should map drilldown response from use case")
  void shouldMapDrilldownResponseFromService() {
    UUID id = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-07-26T08:00:00Z");
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(
                AiDomain.TOOLS,
                "tools.weather",
                Latency.ofMillis(25),
                OwnerKey.forClient("11111111-1111-4111-8111-111111111111"))
            .id(id)
            .occurredAt(occurredAt)
            .provider("openai")
            .model("gpt")
            .sessionId("s1")
            .toolName("weather")
            .tokens(new TokenUsage(11, 22))
            .build();
    when(metricsService.getDrilldown(
            "tools", null, null, null, null, null, null, null, 0, 20, "7d"))
        .thenReturn(new DrilldownPage(List.of(event), 1, 0, 20));

    assertThat(
            mvc.get()
                .uri("/api/metrics/drilldown")
                .param("domain", "tools")
                .param("page", "0")
                .param("size", "20")
                .param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.total")
        .convertTo(Integer.class)
        .isEqualTo(1);

    assertThat(
            mvc.get()
                .uri("/api/metrics/drilldown")
                .param("domain", "tools")
                .param("page", "0")
                .param("size", "20")
                .param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.items[0].toolName")
        .asString()
        .isEqualTo("weather");

    assertThat(
            mvc.get()
                .uri("/api/metrics/drilldown")
                .param("domain", "tools")
                .param("page", "0")
                .param("size", "20")
                .param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .doesNotHavePath("$.items[0].ownerKey");
  }

  @Test
  @DisplayName("should serialize occurredAt as an ISO-8601 UTC string")
  void shouldSerializeOccurredAtAsAnIso8601UtcString() {
    AiInvocationEvent event =
        AiInvocationEvent.succeeded(
                AiDomain.TOOLS, "tools.weather", Latency.ofMillis(25), OwnerKey.UNOWNED)
            .id(UUID.randomUUID())
            .occurredAt(Instant.parse("2026-07-26T08:00:00.123Z"))
            .build();
    when(metricsService.getDrilldown(
            "tools", null, null, null, null, null, null, null, 0, 20, "7d"))
        .thenReturn(new DrilldownPage(List.of(event), 1, 0, 20));

    assertThat(
            mvc.get()
                .uri("/api/metrics/drilldown")
                .param("domain", "tools")
                .param("page", "0")
                .param("size", "20")
                .param("range", "7d"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.items[0].occurredAt")
        .asString()
        .isEqualTo("2026-07-26T08:00:00.123Z");
  }
}
