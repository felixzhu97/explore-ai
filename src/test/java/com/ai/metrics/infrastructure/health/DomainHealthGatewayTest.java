package com.ai.metrics.infrastructure.health;

import com.ai.pipeline.application.usecase.PipelineFacade;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.mcp.application.usecase.McpFacade;
import com.ai.mcp.domain.vo.McpServerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DomainHealthGateway")
class DomainHealthGatewayTest {

    @Mock
    private PipelineFacade pipelineFacade;

    @Mock
    private McpFacade mcpFacade;

    private DomainHealthGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new DomainHealthGateway(pipelineFacade, mcpFacade);
    }

    @Test
    @DisplayName("should_report_system_status_up")
    void should_report_system_status_up() {
        Map<String, Object> status = gateway.systemStatus();

        assertThat(status).containsEntry("status", "UP");
    }

    @Test
    @DisplayName("should_report_agents_up_when_all_registered_agents_are_healthy")
    void should_report_agents_up_when_all_registered_agents_are_healthy() {
        when(pipelineFacade.listAgents(isNull(), eq("en"))).thenReturn(List.of(
                AgentDefinition.create(AgentType.of("researcher"), "Researcher", "desc", "prompt"),
                AgentDefinition.create(AgentType.supervisor(), "Supervisor", "desc", "prompt")));

        Map<String, Object> health = gateway.agentsHealth();

        assertThat(health).containsEntry("status", "UP");
        assertThat(health).containsEntry("agentCount", 2);
        assertThat(health).containsEntry("healthyAgentCount", 2L);
    }

    @Test
    @DisplayName("should_report_agents_degraded_when_list_empty_or_unhealthy")
    void should_report_agents_degraded_when_list_empty_or_unhealthy() {
        when(pipelineFacade.listAgents(isNull(), eq("en"))).thenReturn(List.of());

        Map<String, Object> emptyHealth = gateway.agentsHealth();
        assertThat(emptyHealth).containsEntry("status", "DEGRADED");
        assertThat(emptyHealth).containsEntry("agentCount", 0);

        AgentDefinition unhealthy = mock(AgentDefinition.class);
        when(unhealthy.healthy()).thenReturn(false);
        when(pipelineFacade.listAgents(isNull(), eq("en"))).thenReturn(List.of(unhealthy));

        Map<String, Object> degradedHealth = gateway.agentsHealth();
        assertThat(degradedHealth).containsEntry("status", "DEGRADED");
        assertThat(degradedHealth).containsEntry("healthyAgentCount", 0L);
    }

    @Test
    @DisplayName("should_report_mcp_health_with_tool_and_server_counts")
    void should_report_mcp_health_with_tool_and_server_counts() {
        when(mcpFacade.getTotalToolCount()).thenReturn(5);
        when(mcpFacade.getConnectedServers()).thenReturn(Map.of(
                "weather", McpServerConnection.connected("weather", 2),
                "rag", McpServerConnection.connected("rag", 3)));

        Map<String, Object> health = gateway.mcpHealth();

        assertThat(health).containsEntry("status", "UP");
        assertThat(health).containsEntry("registeredTools", 5);
        assertThat(health).containsEntry("connectedServers", 2);
    }
}
