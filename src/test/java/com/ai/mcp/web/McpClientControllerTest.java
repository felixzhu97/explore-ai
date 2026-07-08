package com.ai.mcp.web;

import com.ai.mcp.application.usecase.McpFacade;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.vo.McpServerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpClientController")
class McpClientControllerTest {

    @Mock
    private McpFacade mcpFacade;

    private McpClientController controller;

    @BeforeEach
    void setUp() {
        controller = new McpClientController(mcpFacade);
    }

    @Nested
    @DisplayName("GET /api/mcp/client/status")
    class GetStatus {

        @Test
        @DisplayName("should return READY status with tool count")
        void should_return_ready_status_with_tool_count() {
            when(mcpFacade.getTotalToolCount()).thenReturn(5);

            var response = controller.getStatus();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("registeredTools", 5);
        }
    }

    @Nested
    @DisplayName("GET /api/mcp/client/servers")
    class ListServers {

        @Test
        @DisplayName("should return list of connected servers")
        void should_return_list_of_connected_servers() {
            when(mcpFacade.getConnectedServers()).thenReturn(Map.of(
                    "server1", McpServerConnection.connected("server1", 3)));

            var response = controller.listServers();

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().getFirst().get("status")).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("GET /api/mcp/client/tools")
    class ListTools {

        @Test
        @DisplayName("should return list of registered MCP tools")
        void should_return_list_of_registered_mcp_tools() {
            when(mcpFacade.getToolDefinitions()).thenReturn(List.of(
                    McpToolDefinition.create("get_weather", "Get current weather")));

            var response = controller.listTools();

            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().getFirst().get("name")).isEqualTo("get_weather");
        }
    }

    @Nested
    @DisplayName("POST /api/mcp/client/chat")
    class Chat {

        @Test
        @DisplayName("should return internal server error on service exception")
        void should_return_internal_server_error_on_service_exception() {
            when(mcpFacade.chatWithTools("Hello")).thenThrow(new RuntimeException("Service error"));

            var response = controller.chat(new McpClientController.McpChatRequest("Hello", null));

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
