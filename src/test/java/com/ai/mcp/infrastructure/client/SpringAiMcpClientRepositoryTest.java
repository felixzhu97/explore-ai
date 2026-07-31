package com.ai.mcp.infrastructure.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpringAiMcpClientRepository")
class SpringAiMcpClientRepositoryTest {

    private SpringAiMcpClientRepository repository;

    @BeforeEach
    void setUp() {
        repository = new SpringAiMcpClientRepository();
    }

    @Nested
    @DisplayName("registerToolCallbacks")
    class RegisterToolCallbacksTests {

        @Test
        @DisplayName("should register tools from MCP server")
        void should_register_tools_from_mcp_server() {
            repository.registerToolCallbacks(createMockTools("tool1", "tool2"), "test-server");

            assertThat(repository.toolCount()).isEqualTo(2);
            assertThat(repository.listServers().keySet()).contains("test-server");
        }

        @Test
        @DisplayName("should accumulate tools from multiple servers")
        void should_accumulate_tools_from_multiple_servers() {
            repository.registerToolCallbacks(createMockTools("tool1", "tool2"), "server1");
            repository.registerToolCallbacks(createMockTools("tool3", "tool4"), "server2");

            assertThat(repository.toolCount()).isEqualTo(4);
            assertThat(repository.listServers().keySet()).hasSize(2);
        }

        @Test
        @DisplayName("should replace tools when same server reregisters")
        void should_replace_tools_when_same_server_reregisters() {
            repository.registerToolCallbacks(createMockTools("tool1", "tool2"), "test-server");
            repository.registerToolCallbacks(createMockTools("tool3"), "test-server");

            assertThat(repository.toolCount()).isEqualTo(1);
            assertThat(repository.listTools()).extracting("name").containsExactly("tool3");
            assertThat(repository.listServers().keySet()).contains("test-server");
        }
    }

    @Nested
    @DisplayName("clearTools")
    class ClearToolsTests {

        @Test
        @DisplayName("should clear all registered tools and servers")
        void should_clear_all_registered_tools() {
            repository.registerToolCallbacks(createMockTools("tool1", "tool2"), "server1");

            repository.clearTools();

            assertThat(repository.toolCount()).isEqualTo(0);
            assertThat(repository.listServers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("resources and prompts")
    class ResourcesAndPromptsTests {

        @Test
        @DisplayName("should_list_resources_and_prompts_per_server")
        void should_list_resources_and_prompts_per_server() {
            repository.registerToolCallbacks(createMockTools("tool1"), "server1");
            repository.registerResources(
                    List.of(com.ai.mcp.domain.model.McpResourceDefinition.create(
                            "config:///k", "Config", "desc", "server1")),
                    "server1");
            repository.registerPrompts(
                    List.of(com.ai.mcp.domain.model.McpPromptDefinition.create(
                            "greeting", "hi", "server1")),
                    "server1");
            repository.updateServerCapabilities("server1", true, true, true);

            assertThat(repository.listResources()).hasSize(1);
            assertThat(repository.listPrompts()).hasSize(1);
            assertThat(repository.listTools().getFirst().serverName()).isEqualTo("server1");
            assertThat(repository.listServers().get("server1").resourcesSupported()).isTrue();
            assertThat(repository.listServers().get("server1").promptsSupported()).isTrue();
        }
    }

    private ToolCallback[] createMockTools(String... names) {
        ToolCallback[] tools = new ToolCallback[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            tools[i] = new ToolCallback() {
                @Override
                public ToolDefinition getToolDefinition() {
                    return ToolDefinition.builder()
                            .name(name)
                            .description("desc")
                            .inputSchema("{}")
                            .build();
                }

                @Override
                public String call(String toolInput) {
                    return "result";
                }
            };
        }
        return tools;
    }
}
