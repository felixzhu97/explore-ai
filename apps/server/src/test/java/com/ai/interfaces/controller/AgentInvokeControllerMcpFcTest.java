package com.ai.interfaces.controller;

import com.ai.application.port.ToolRegistryPort;
import com.ai.domain.service.AiChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke tests for the MCP and FC tab endpoints in {@link AgentInvokeController}.
 *
 * <p>These tests verify that endpoints exist (return non-404) and unknown tabs
 * return 400. Full streaming/ChatClient integration requires a running server.</p>
 *
 * @since 0.2.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentInvokeController MCP/FC Tab Tests")
class AgentInvokeControllerMcpFcTest {

    private MockMvc mockMvc;

    @Mock
    private ToolRegistryPort toolRegistry;

    @Mock
    private AiChatService aiChatService;

    private AgentInvokeController createController() {
        return new AgentInvokeController(toolRegistry, aiChatService);
    }

    private MockMvc createMockMvc(AgentInvokeController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Nested
    @DisplayName("POST /api/agents/{tab}/invoke/sse")
    class TabInvoke {

        @Test
        @DisplayName("should return 200 for known tab 'supervisor'")
        void shouldReturn200ForSupervisorTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/supervisor/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"health check\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 for unknown tab")
        void shouldReturn400ForUnknownTab() throws Exception {
            AgentInvokeController ctrl = createController();
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/nonexistent/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"test\"}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 for tab 'kubernetes'")
        void shouldReturn200ForKubernetesTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/kubernetes/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"check pods\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 for tab 'monitoring'")
        void shouldReturn200ForMonitoringTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/monitoring/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"check metrics\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 for tab 'model'")
        void shouldReturn200ForModelTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/model/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"evaluate\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 for tab 'llmops'")
        void shouldReturn200ForLlmoopsTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/llmops/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"stats\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 for tab 'aiops'")
        void shouldReturn200ForAiopSTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/aiops/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"detect anomaly\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 for tab 'vector'")
        void shouldReturn200ForVectorTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/vector/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"inspect db\"}"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/agents/mcp/invoke/sse")
    class McpTab {

        @Test
        @DisplayName("should return 200 (not 404) for MCP tab")
        void shouldReturn200ForMcpTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/mcp/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"list tools\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 with empty message")
        void shouldReturn200WithEmptyMessage() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/mcp/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 with sessionId in body")
        void shouldReturn200WithSessionId() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/mcp/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"check\",\"sessionId\":\"sess-123\"}"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/agents/fc/invoke/sse")
    class FcTab {

        @Test
        @DisplayName("should return 200 (not 404) for FC tab")
        void shouldReturn200ForFcTab() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/fc/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"what is the cpu usage\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 with toolName parameter")
        void shouldReturn200WithToolName() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/fc/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"cpu\",\"toolName\":\"get_cpu\"}"))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 for FC tab with sessionId")
        void shouldReturn200WithSessionId() throws Exception {
            AgentInvokeController ctrl = createController();
            when(aiChatService.chatStream(anyString(), anyString()))
                .thenReturn(Flux.empty());
            mockMvc = createMockMvc(ctrl);

            mockMvc.perform(post("/api/agents/fc/invoke/sse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"search docs\",\"sessionId\":\"sess-456\",\"toolName\":\"rag_search\"}"))
                .andExpect(status().isOk());
        }
    }
}
