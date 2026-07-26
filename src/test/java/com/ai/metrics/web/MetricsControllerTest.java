package com.ai.metrics.web;

import com.ai.metrics.application.model.DrilldownPage;
import com.ai.metrics.application.model.MetricsDomainSnapshot;
import com.ai.metrics.application.model.MetricsOverview;
import com.ai.metrics.application.model.NamedCount;
import com.ai.metrics.application.model.SeriesPoint;
import com.ai.metrics.application.model.SeriesSnapshot;
import com.ai.metrics.application.usecase.MetricsUseCase;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.web.dto.DrilldownPageResponse;
import com.ai.metrics.web.dto.MetricsDomainResponse;
import com.ai.metrics.web.dto.MetricsOverviewResponse;
import com.ai.metrics.web.dto.SeriesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MetricsController")
class MetricsControllerTest {

    @Mock
    private MetricsUseCase metricsUseCase;

    private MetricsController controller;

    @BeforeEach
    void setUp() {
        controller = new MetricsController(metricsUseCase);
    }

    @Test
    @DisplayName("should_map_overview_response_from_use_case")
    void should_map_overview_response_from_use_case() {
        MetricsOverview overview = new MetricsOverview(
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

        ResponseEntity<MetricsOverviewResponse> response = controller.overview("7d");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        MetricsOverviewResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.range()).isEqualTo("7d");
        assertThat(body.requestCount()).isEqualTo(100);
        assertThat(body.errorCount()).isEqualTo(5);
        assertThat(body.requestsByDomain()).extracting(nc -> nc.name()).containsExactly("chat");
        verify(metricsUseCase).overview("7d");
    }

    @Test
    @DisplayName("should_map_domain_response_from_use_case")
    void should_map_domain_response_from_use_case() {
        MetricsDomainSnapshot snapshot = new MetricsDomainSnapshot(
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

        ResponseEntity<MetricsDomainResponse> response = controller.domain("chat", "7d");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().domain()).isEqualTo("chat");
        assertThat(response.getBody().inventory()).containsEntry("sessionCount", 3);
        assertThat(response.getBody().requestSeries()).hasSize(1);
        assertThat(response.getBody().modelSeries()).extracting(p -> p.label()).containsExactly("gpt");
    }

    @Test
    @DisplayName("should_map_series_response_from_use_case")
    void should_map_series_response_from_use_case() {
        SeriesSnapshot snapshot = new SeriesSnapshot(
                "requests",
                "chat",
                "7d",
                List.of(new SeriesPoint("2026-07-01", 9)));
        when(metricsUseCase.series("requests", "chat", "7d")).thenReturn(snapshot);

        ResponseEntity<SeriesResponse> response = controller.series("requests", "chat", "7d");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("requests");
        assertThat(response.getBody().points()).extracting(p -> p.label()).containsExactly("2026-07-01");
    }

    @Test
    @DisplayName("should_map_drilldown_response_from_use_case")
    void should_map_drilldown_response_from_use_case() {
        UUID id = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-26T08:00:00Z");
        AiInvocationEvent event = AiInvocationEvent.builder()
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
        when(metricsUseCase.drilldown(
                "tools", null, null, null, null, null, null, null, 0, 20, "7d"))
                .thenReturn(new DrilldownPage(List.of(event), 1, 0, 20));

        ResponseEntity<DrilldownPageResponse> response = controller.drilldown(
                "tools", null, null, null, null, null, null, null, 0, 20, "7d");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().total()).isEqualTo(1);
        assertThat(response.getBody().items()).hasSize(1);
        assertThat(response.getBody().items().getFirst().id()).isEqualTo(id.toString());
        assertThat(response.getBody().items().getFirst().toolName()).isEqualTo("weather");
    }
}
