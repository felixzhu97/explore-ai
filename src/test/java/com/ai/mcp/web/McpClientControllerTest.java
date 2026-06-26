package com.ai.mcp.web;

import com.ai.mcp.client.AiMcpClientService;
import com.ai.mcp.web.McpClientController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpClientController")
class McpClientControllerTest {

    @Mock
    private AiMcpClientService mcpClientService;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    private McpClientController controller;

    @BeforeEach
    void setUp() {
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        controller = new McpClientController(mcpClientService, chatClientBuilder);
    }

    @Nested
    @DisplayName("GET /api/mcp/client/status")
    class GetStatus {

        @Test
        @DisplayName("should return READY status with tool count")
        void shouldReturnReadyStatusWithToolCount() {
            when(mcpClientService.getTotalToolCount()).thenReturn(5);

            var response = controller.getStatus();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("READY");
            assertThat(response.getBody().get("registeredTools")).isEqualTo(5);
        }

        @Test
        @DisplayName("should return zero tools when no tools registered")
        void shouldReturnZeroToolsWhenNoToolsRegistered() {
            when(mcpClientService.getTotalToolCount()).thenReturn(0);

            var response = controller.getStatus();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().get("registeredTools")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GET /api/mcp/client/servers")
    class ListServers {

        @Test
        @DisplayName("should return list of connected servers")
        void shouldReturnListOfConnectedServers() {
            AiMcpClientService.ServerInfo server1 = new AiMcpClientService.ServerInfo("server1", 3, "CONNECTED");
            when(mcpClientService.getConnectedServers()).thenReturn(
                    Map.of("server1", server1)
            );

            var response = controller.listServers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).get("name")).isEqualTo("server1");
            assertThat(response.getBody().get(0).get("toolCount")).isEqualTo(3);
            assertThat(response.getBody().get(0).get("status")).isEqualTo("CONNECTED");
        }

        @Test
        @DisplayName("should return empty list when no servers connected")
        void shouldReturnEmptyListWhenNoServersConnected() {
            when(mcpClientService.getConnectedServers()).thenReturn(Map.of());

            var response = controller.listServers();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/mcp/client/tools")
    class ListTools {

        @Test
        @DisplayName("should return list of registered MCP tools")
        void shouldReturnListOfRegisteredMcpTools() {
            var toolDef1 = mock(ToolDefinition.class);
            when(toolDef1.name()).thenReturn("get_weather");
            when(toolDef1.description()).thenReturn("Get current weather");

            var toolDef2 = mock(ToolDefinition.class);
            when(toolDef2.name()).thenReturn("search_docs");
            when(toolDef2.description()).thenReturn("Search documents");

            when(mcpClientService.getToolDefinitions()).thenReturn(List.of(toolDef1, toolDef2));

            var response = controller.listTools();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).get("name")).isEqualTo("get_weather");
            assertThat(response.getBody().get(0).get("description")).isEqualTo("Get current weather");
            assertThat(response.getBody().get(1).get("name")).isEqualTo("search_docs");
        }

        @Test
        @DisplayName("should return empty list when no tools registered")
        void shouldReturnEmptyListWhenNoToolsRegistered() {
            when(mcpClientService.getToolDefinitions()).thenReturn(List.of());

            var response = controller.listTools();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("should handle null description gracefully")
        void shouldHandleNullDescriptionGracefully() {
            var toolDef = mock(ToolDefinition.class);
            when(toolDef.name()).thenReturn("no_desc_tool");
            when(toolDef.description()).thenReturn(null);
            when(mcpClientService.getToolDefinitions()).thenReturn(List.of(toolDef));

            var response = controller.listTools();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).get("description")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("POST /api/mcp/client/chat")
    class Chat {

        @Test
        @DisplayName("should return bad request on service exception")
        void shouldReturnBadRequestOnServiceException() {
            when(mcpClientService.getRegisteredTools()).thenThrow(new RuntimeException("Service error"));

            var request = new McpClientController.McpChatRequest("Hello", null);
            var response = controller.chat(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Service error");
        }
    }
}
