package com.ai.modules.mcp.client;

import com.ai.modules.mcp.client.AiMcpClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AiMcpClientService Tests")
class AiMcpClientServiceTest {

    private AiMcpClientService service;

    @BeforeEach
    void setUp() {
        service = new AiMcpClientService();
    }

    @Nested
    @DisplayName("registerTools")
    class RegisterToolsTests {

        @Test
        @DisplayName("should register tools from MCP server")
        void shouldRegisterToolsFromMcpServer() {
            ToolCallback[] tools = createMockTools("tool1", "tool2");

            service.registerTools(tools, "test-server");

            assertThat(service.getTotalToolCount()).isEqualTo(2);
            assertThat(service.getConnectedServers().keySet()).contains("test-server");
        }

        @Test
        @DisplayName("should accumulate tools from multiple servers")
        void shouldAccumulateToolsFromMultipleServers() {
            service.registerTools(createMockTools("tool1", "tool2"), "server1");
            service.registerTools(createMockTools("tool3", "tool4"), "server2");

            assertThat(service.getTotalToolCount()).isEqualTo(4);
            assertThat(service.getConnectedServers().keySet()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getConnectedServers")
    class GetConnectedServersTests {

        @Test
        @DisplayName("should return server info")
        void shouldReturnServerInfo() {
            service.registerTools(createMockTools("tool1"), "server1");
            service.registerTools(createMockTools("tool1", "tool2"), "server2");

            var servers = service.getConnectedServers();

            assertThat(servers.keySet()).hasSize(2);
            assertThat(servers.get("server1").toolCount()).isEqualTo(1);
            assertThat(servers.get("server2").toolCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("clearTools")
    class ClearToolsTests {

        @Test
        @DisplayName("should clear all registered tools and servers")
        void shouldClearAllRegisteredTools() {
            service.registerTools(createMockTools("tool1", "tool2"), "server1");

            service.clearTools();

            assertThat(service.getTotalToolCount()).isEqualTo(0);
            assertThat(service.getConnectedServers().keySet()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRegisteredTools")
    class GetRegisteredToolsTests {

        @Test
        @DisplayName("should return copy of registered tools list")
        void shouldReturnCopyOfRegisteredTools() {
            service.registerTools(createMockTools("tool1", "tool2"), "server1");

            List<ToolCallback> tools = service.getRegisteredTools();

            assertThat(tools).hasSize(2);
        }
    }

    private ToolCallback[] createMockTools(String... names) {
        return java.util.Arrays.stream(names)
                .map(MockToolCallback::new)
                .toArray(ToolCallback[]::new);
    }

    private static class MockToolCallback implements ToolCallback {
        private final String name;

        MockToolCallback(String name) {
            this.name = name;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder()
                    .name(name)
                    .description("Mock tool")
                    .build();
        }

        @Override
        public String call(String functionInput) {
            return "mock response";
        }
    }
}
