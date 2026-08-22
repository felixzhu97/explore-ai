package com.ai.metrics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.service.model.DrilldownPage;
import com.ai.metrics.service.model.MetricsDomainSnapshot;
import com.ai.metrics.service.model.MetricsOverview;
import com.ai.metrics.service.model.NamedCount;
import com.ai.metrics.service.model.SeriesPoint;
import com.ai.metrics.service.model.SeriesSnapshot;
import com.ai.metrics.service.usecase.MetricsUseCase;
import com.ai.testsupport.SliceWebMvcTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = MetricsController.class)
@DisplayName("MetricsController")
class MetricsControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private MetricsUseCase metricsUseCase;

  @Nested
  @DisplayName("GET /api/metrics/overview")
  class Overview {

    @Test
    @DisplayName("should map overview response from use case")
    void shouldMapOverviewResponseFromUseCase() {
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
              Map.of("chat", Map.of("status", "UP")));
      when(metricsUseCase.overview("7d")).thenReturn(overview);

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
      verify(metricsUseCase).overview("7d");
    }
  }

  @Nested
  @DisplayName("GET /api/metrics/domains/{domain}")
  class Domain {

    @Test
    @DisplayName("should map domain response from use case")
    void shouldMapDomainResponseFromUseCase() {
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
              Map.of("sessionCount", 3),
              List.of(new SeriesPoint("2026-07-01", 5)),
              List.of(new SeriesPoint("gpt", 4)));
      when(metricsUseCase.domain("chat", "7d")).thenReturn(snapshot);

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
  }

  @Nested
  @DisplayName("GET /api/metrics/series")
  class Series {

    @Test
    @DisplayName("should map series response from use case")
    void shouldMapSeriesResponseFromUseCase() {
      SeriesSnapshot snapshot =
          new SeriesSnapshot("requests", "chat", "7d", List.of(new SeriesPoint("2026-07-01", 9)));
      when(metricsUseCase.series("requests", "chat", "7d")).thenReturn(snapshot);

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
  }

  @Nested
  @DisplayName("GET /api/metrics/drilldown")
  class Drilldown {

    @Test
    @DisplayName("should map drilldown response from use case")
    void shouldMapDrilldownResponseFromUseCase() {
      UUID id = UUID.randomUUID();
      Instant occurredAt = Instant.parse("2026-07-26T08:00:00Z");
      AiInvocationEvent event =
          AiInvocationEvent.builder()
              .id(id)
              .occurredAt(occurredAt)
              .domain(AiDomain.TOOLS)
              .operation("tools.weather")
              .outcome(InvocationOutcome.SUCCESS)
              .latencyMs(25)
              .provider("openai")
              .model("gpt")
              .sessionId("s1")
              .toolName("weather")
              .promptTokens(11)
              .completionTokens(22)
              .build();
      when(metricsUseCase.drilldown("tools", null, null, null, null, null, null, null, 0, 20, "7d"))
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
    }
  }
}
