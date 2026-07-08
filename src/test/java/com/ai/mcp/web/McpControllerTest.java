package com.ai.mcp.web;

import com.ai.mcp.web.McpController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpController")
class McpControllerTest {

    private final McpController controller = new McpController();

    @Nested
    @DisplayName("GET /api/mcp/health")
    class Health {

        @Test
        @DisplayName("should return UP status")
        void shouldReturnUpStatus() {
            var response = controller.health();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("UP");
            assertThat(response.getBody().get("server")).isEqualTo("explore-ai-mcp-server");
            assertThat(response.getBody().get("version")).isEqualTo("1.0.0");
            assertThat(response.getBody().get("protocol")).isEqualTo("MCP 1.0");
        }
    }

    @Nested
    @DisplayName("GET /api/mcp/info")
    class Info {

        @Test
        @DisplayName("should return complete MCP server information")
        void shouldReturnCompleteMcpServerInformation() {
            var response = controller.info();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            // Verify root level fields
            assertThat(response.getBody().get("name")).isEqualTo("explore-ai-mcp-server");
            assertThat(response.getBody().get("version")).isEqualTo("1.0.0");
            assertThat(response.getBody().get("description")).isEqualTo("AI Explore MCP Server with RAG, Weather, and Chat tools");

            // Verify capabilities
            @SuppressWarnings("unchecked")
            Map<String, Boolean> capabilities = (Map<String, Boolean>) response.getBody().get("capabilities");
            assertThat(capabilities).isNotNull();
            assertThat(capabilities.get("tools")).isTrue();
            assertThat(capabilities.get("resources")).isTrue();
            assertThat(capabilities.get("prompts")).isTrue();

            // Verify available tools
            @SuppressWarnings("unchecked")
            Map<String, String> availableTools = (Map<String, String>) response.getBody().get("availableTools");
            assertThat(availableTools).isNotNull();
            assertThat(availableTools).containsKey("get_weather");
            assertThat(availableTools).containsKey("get_forecast");
            assertThat(availableTools).containsKey("search_knowledge_base");
            assertThat(availableTools).containsKey("list_documents");
            assertThat(availableTools).containsKey("ai_chat");

            // Verify available resources
            @SuppressWarnings("unchecked")
            Map<String, String> availableResources = (Map<String, String>) response.getBody().get("availableResources");
            assertThat(availableResources).isNotNull();
            assertThat(availableResources).containsKey("document:///{docId}");
            assertThat(availableResources).containsKey("config:///{key}");

            // Verify available prompts
            @SuppressWarnings("unchecked")
            Map<String, String> availablePrompts = (Map<String, String>) response.getBody().get("availablePrompts");
            assertThat(availablePrompts).isNotNull();
            assertThat(availablePrompts).containsKey("analyze-document");
            assertThat(availablePrompts).containsKey("greeting");
        }
    }
}
