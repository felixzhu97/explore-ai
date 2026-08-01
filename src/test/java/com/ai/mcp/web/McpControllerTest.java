package com.ai.mcp.web;

import com.ai.mcp.infrastructure.server.McpServerCapabilityCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("McpController")
class McpControllerTest {

    private final McpController controller = new McpController(new McpServerCapabilityCatalog());

    @Nested
    @DisplayName("GET /api/mcp/health")
    class Health {

        @Test
        @DisplayName("should return UP status")
        void should_return_up_status() {
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
        @DisplayName("should_return_info_aligned_with_real_server_capabilities")
        void should_return_info_aligned_with_real_server_capabilities() {
            var response = controller.info();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            assertThat(response.getBody().get("name")).isEqualTo("explore-ai-mcp-server");
            assertThat(response.getBody().get("version")).isEqualTo("1.0.0");

            @SuppressWarnings("unchecked")
            Map<String, Boolean> capabilities = (Map<String, Boolean>) response.getBody().get("capabilities");
            assertThat(capabilities).containsEntry("tools", true)
                    .containsEntry("resources", true)
                    .containsEntry("prompts", true);

            @SuppressWarnings("unchecked")
            Map<String, String> availableTools = (Map<String, String>) response.getBody().get("availableTools");
            assertThat(availableTools).containsKeys(
                    "get_weather", "get_forecast", "search_knowledge_base", "list_documents", "ai_chat");

            @SuppressWarnings("unchecked")
            Map<String, String> availableResources = (Map<String, String>) response.getBody().get("availableResources");
            assertThat(availableResources).containsKeys("document:///{docId}", "config:///{key}");

            @SuppressWarnings("unchecked")
            Map<String, String> availablePrompts = (Map<String, String>) response.getBody().get("availablePrompts");
            assertThat(availablePrompts).containsKeys("analyze-document", "greeting");
        }
    }
}
