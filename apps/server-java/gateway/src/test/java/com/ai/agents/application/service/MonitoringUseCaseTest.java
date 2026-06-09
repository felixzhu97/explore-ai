package com.ai.agents.application.service;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.ToolResult;
import com.ai.agents.domain.service.agents.MonitoringAgentService;
import com.ai.agents.infrastructure.tools.MonitoringTools;
import com.ai.agents.presentation.dto.AgentResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringUseCase Tests")
class MonitoringUseCaseTest {

    @Mock
    private MonitoringAgentService domainService;

    @Mock
    private MonitoringTools monitoringTools;

    private MonitoringUseCase monitoringUseCase;

    @BeforeEach
    void setUp() {
        monitoringUseCase = new MonitoringUseCase(domainService, monitoringTools);
    }

    @Nested
    @DisplayName("queryMetrics")
    class QueryMetricsTests {

        @Test
        @DisplayName("should return metrics query result successfully")
        void shouldReturnMetricsQueryResultSuccessfully() {
            String metric = "cpu_usage";
            String timeRange = "5m";
            String aggregation = "avg";
            String expectedContent = "Metric: cpu_usage\\nValue: 75.50";
            when(monitoringTools.queryMetrics(metric, timeRange, aggregation))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(monitoringUseCase.queryMetrics(metric, timeRange, aggregation))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("cpu_usage");
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return success even when tool returns error")
        void shouldReturnSuccessEvenWhenToolReturnsError() {
            String metric = "invalid_metric";
            String timeRange = "5m";
            String aggregation = "avg";
            when(monitoringTools.queryMetrics(metric, timeRange, aggregation))
                    .thenReturn(Mono.just(ToolResult.error("Metric not found")));

            StepVerifier.create(monitoringUseCase.queryMetrics(metric, timeRange, aggregation))
                    .assertNext(response -> {
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should pass correct parameters to monitoring tools")
        void shouldPassCorrectParametersToMonitoringTools() {
            String metric = "memory_usage";
            String timeRange = "1h";
            String aggregation = "max";
            when(monitoringTools.queryMetrics(metric, timeRange, aggregation))
                    .thenReturn(Mono.just(ToolResult.success("result")));

            monitoringUseCase.queryMetrics(metric, timeRange, aggregation).block();

            verify(monitoringTools).queryMetrics(metric, timeRange, aggregation);
        }
    }

    @Nested
    @DisplayName("createAlert")
    class CreateAlertTests {

        @Test
        @DisplayName("should create alert successfully")
        void shouldCreateAlertSuccessfully() {
            String name = "high_cpu_alert";
            String metric = "cpu_usage";
            String condition = ">";
            double threshold = 80.0;
            String expectedContent = "Alert Created: high_cpu_alert";
            when(monitoringTools.createAlert(name, metric, condition, threshold))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(monitoringUseCase.createAlert(name, metric, condition, threshold))
                    .assertNext(response -> {
                        assertThat(response.message()).contains(name);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return success response even when tool returns error")
        void shouldReturnSuccessResponseEvenWhenToolReturnsError() {
            String name = "invalid_alert";
            String metric = "unknown_metric";
            String condition = ">";
            double threshold = 50.0;
            when(monitoringTools.createAlert(name, metric, condition, threshold))
                    .thenReturn(Mono.just(ToolResult.error("Alert creation failed")));

            StepVerifier.create(monitoringUseCase.createAlert(name, metric, condition, threshold))
                    .assertNext(response -> {
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("listAlerts")
    class ListAlertsTests {

        @Test
        @DisplayName("should return alert list successfully")
        void shouldReturnAlertListSuccessfully() {
            String status = "active";
            String expectedContent = "Alert1\\nAlert2";
            when(monitoringTools.listAlerts(status))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(monitoringUseCase.listAlerts(status))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("Alert1");
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return empty list message when no alerts")
        void shouldReturnEmptyListMessageWhenNoAlerts() {
            String status = "resolved";
            String expectedContent = "No alerts found.";
            when(monitoringTools.listAlerts(status))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(monitoringUseCase.listAlerts(status))
                    .assertNext(response -> {
                        assertThat(response.message()).contains("No alerts");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should handle null status parameter")
        void shouldHandleNullStatusParameter() {
            when(monitoringTools.listAlerts(null))
                    .thenReturn(Mono.just(ToolResult.success("All alerts")));

            StepVerifier.create(monitoringUseCase.listAlerts(null))
                    .assertNext(response -> {
                        assertThat(response.message()).isNotNull();
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("fireAlert")
    class FireAlertTests {

        @Test
        @DisplayName("should fire alert successfully")
        void shouldFireAlertSuccessfully() {
            String name = "critical_alert";
            String message = "CPU usage exceeded threshold";
            String expectedContent = "Alert Fired: critical_alert";
            when(monitoringTools.fireAlert(name, message))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(monitoringUseCase.fireAlert(name, message))
                    .assertNext(response -> {
                        assertThat(response.message()).contains(name);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return success response even when tool returns error")
        void shouldReturnSuccessResponseEvenWhenToolReturnsError() {
            String name = "non_existent_alert";
            String message = "This alert does not exist";
            when(monitoringTools.fireAlert(name, message))
                    .thenReturn(Mono.just(ToolResult.error("Alert not found")));

            StepVerifier.create(monitoringUseCase.fireAlert(name, message))
                    .assertNext(response -> {
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("resolveAlert")
    class ResolveAlertTests {

        @Test
        @DisplayName("should resolve alert successfully")
        void shouldResolveAlertSuccessfully() {
            String name = "resolved_alert";
            String message = "Issue fixed";
            String expectedContent = "Alert Resolved: resolved_alert";
            when(monitoringTools.resolveAlert(name, message))
                    .thenReturn(Mono.just(ToolResult.success(expectedContent)));

            StepVerifier.create(monitoringUseCase.resolveAlert(name, message))
                    .assertNext(response -> {
                        assertThat(response.message()).contains(name);
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should return success response even when tool returns error")
        void shouldReturnSuccessResponseEvenWhenToolReturnsError() {
            String name = "non_existent";
            String message = "Cannot resolve";
            when(monitoringTools.resolveAlert(name, message))
                    .thenReturn(Mono.just(ToolResult.error("Alert not found")));

            StepVerifier.create(monitoringUseCase.resolveAlert(name, message))
                    .assertNext(response -> {
                        assertThat(response.agentType()).isEqualTo(AgentType.SUPERVISOR);
                    })
                    .verifyComplete();
        }
    }
}
