package com.ai.metrics.infra.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ai.metrics.domain.model.ModuleStatus;
import com.ai.metrics.domain.repository.McpHealthProbe;
import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.repository.MetricsHealthGateway.McpHealth;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import com.ai.pipeline.service.PipelineService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainHealthGateway")
class DomainHealthGatewayTest {

  @Mock private PipelineService pipelineService;

  @Mock private McpHealthProbe mcpHealthProbe;

  @Mock private ObjectProvider<McpHealthProbe> mcpHealthProbeProvider;

  private DomainHealthGateway gateway;

  @BeforeEach
  void setUp() {
    gateway = new DomainHealthGateway(pipelineService, mcpHealthProbeProvider);
  }

  @Test
  @DisplayName("should report system status up")
  void shouldReportSystemStatusUp() {
    assertThat(gateway.getSystemStatus()).isEqualTo(ModuleStatus.UP);
  }

  @Test
  @DisplayName("should report agents up when all registered agents are healthy")
  void shouldReportAgentsUpWhenAllRegisteredAgentsAreHealthy() {
    when(pipelineService.listAgents(isNull(), eq("en")))
        .thenReturn(
            List.of(
                AgentDefinition.create(AgentType.of("researcher"), "Researcher", "desc", "prompt"),
                AgentDefinition.create(AgentType.supervisor(), "Supervisor", "desc", "prompt")));

    assertThat(gateway.checkAgentsHealth()).isEqualTo(new AgentsHealth(ModuleStatus.UP, 2, 2));
  }

  @Test
  @DisplayName("should report agents degraded when list empty or unhealthy")
  void shouldReportAgentsDegradedWhenListEmptyOrUnhealthy() {
    when(pipelineService.listAgents(isNull(), eq("en"))).thenReturn(List.of());

    assertThat(gateway.checkAgentsHealth())
        .isEqualTo(new AgentsHealth(ModuleStatus.DEGRADED, 0, 0));

    AgentDefinition unhealthy = mock(AgentDefinition.class);
    when(unhealthy.isHealthy()).thenReturn(false);
    when(pipelineService.listAgents(isNull(), eq("en"))).thenReturn(List.of(unhealthy));

    assertThat(gateway.checkAgentsHealth())
        .isEqualTo(new AgentsHealth(ModuleStatus.DEGRADED, 1, 0));
  }

  @Test
  @DisplayName("should report mcp health with tool and server counts")
  void shouldReportMcpHealthWithToolAndServerCounts() {
    when(mcpHealthProbeProvider.getIfAvailable()).thenReturn(mcpHealthProbe);
    when(mcpHealthProbe.countRegisteredTools()).thenReturn(5);
    when(mcpHealthProbe.countConnectedServers()).thenReturn(2);

    assertThat(gateway.checkMcpHealth()).isEqualTo(new McpHealth(ModuleStatus.UP, 5, 2));
  }

  @Test
  @DisplayName("should report mcp disabled when mcp service bean is absent")
  void shouldReportMcpDisabledWhenServiceBeanIsAbsent() {
    when(mcpHealthProbeProvider.getIfAvailable()).thenReturn(null);

    assertThat(gateway.checkMcpHealth()).isEqualTo(new McpHealth(ModuleStatus.DISABLED, 0, 0));
  }
}
