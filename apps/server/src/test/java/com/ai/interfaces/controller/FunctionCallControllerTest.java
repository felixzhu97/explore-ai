package com.ai.interfaces.controller;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for {@link FunctionCallController}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FunctionCallController Tests")
class FunctionCallControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ToolRegistryPort toolRegistry;

    @InjectMocks
    private FunctionCallController controller;

    private ToolDefinition ragToolDef;
    private ToolDefinition compositeToolDef;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        ragToolDef = ToolDefinition.atomic(
            "rag_search",
            "Search knowledge base",
            Map.of("type", "object", "properties", Map.of("query", Map.of("type", "string"))),
            "RAG"
        );
        ToolDefinition cpuToolDef = ToolDefinition.atomic(
            "get_cpu",
            "Get CPU usage",
            Map.of("type", "object", "properties", Map.of()),
            "Monitor"
        );
        compositeToolDef = ToolDefinition.composite(
            "supervisor_overview",
            "System overview",
            Map.of("type", "object", "properties", Map.of()),
            "Composite"
        );

        when(toolRegistry.listTools()).thenReturn(List.of(ragToolDef, cpuToolDef, compositeToolDef));
        when(toolRegistry.findByName("rag_search")).thenReturn(Optional.of(ragToolDef));
        when(toolRegistry.findByName("supervisor_overview")).thenReturn(Optional.of(compositeToolDef));
    }

    @Nested
    @DisplayName("GET /api/function-call/tools")
    class ListTools {

        @Test
        @DisplayName("should return 200 with list of tools")
        void shouldReturnToolList() throws Exception {
            mockMvc.perform(get("/api/function-call/tools")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("rag_search"))
                .andExpect(jsonPath("$[0].composite").value(false))
                .andExpect(jsonPath("$[0].category").value("RAG"))
                .andExpect(jsonPath("$[2].name").value("supervisor_overview"))
                .andExpect(jsonPath("$[2].composite").value(true))
                .andExpect(jsonPath("$[2].category").value("Composite"));
        }

        @Test
        @DisplayName("should return empty list when no tools registered")
        void shouldReturnEmptyList() throws Exception {
            when(toolRegistry.listTools()).thenReturn(List.of());

            mockMvc.perform(get("/api/function-call/tools")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/function-call/invoke")
    class Invoke {

        @Test
        @DisplayName("should return 200 with tool result for valid request")
        void shouldInvokeTool() throws Exception {
            ToolResult successResult = ToolResult.success("CPU load: 45%");
            when(toolRegistry.invoke(any(ToolInvocation.class))).thenReturn(successResult);

            String body = "{\"toolName\":\"get_cpu\",\"arguments\":{\"sample_seconds\":1}}";

            mockMvc.perform(post("/api/function-call/invoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("CPU load: 45%"))
                .andExpect(jsonPath("$.isError").value(false));
        }

        @Test
        @DisplayName("should return 200 with isError=true when registry returns error result")
        void shouldReturnErrorResult() throws Exception {
            ToolResult errorResult = ToolResult.error("Tool execution failed: timeout");
            when(toolRegistry.invoke(any(ToolInvocation.class))).thenReturn(errorResult);

            String body = "{\"toolName\":\"get_cpu\",\"arguments\":{}}";

            mockMvc.perform(post("/api/function-call/invoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Tool execution failed: timeout"))
                .andExpect(jsonPath("$.isError").value(true));
        }

        @Test
        @DisplayName("should return 200 with isError=true for unknown tool (not 404)")
        void shouldReturnErrorForUnknownTool() throws Exception {
            when(toolRegistry.invoke(any(ToolInvocation.class)))
                .thenReturn(ToolResult.error("Tool not found: unknown_tool"));

            String body = "{\"toolName\":\"unknown_tool\",\"arguments\":{}}";

            mockMvc.perform(post("/api/function-call/invoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isError").value(true))
                .andExpect(jsonPath("$.content").value("Tool not found: unknown_tool"));
        }

        @Test
        @DisplayName("should return 400 for missing toolName")
        void shouldReturn400ForMissingToolName() throws Exception {
            String body = "{\"arguments\": {}}";

            mockMvc.perform(post("/api/function-call/invoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 for blank toolName")
        void shouldReturn400ForBlankToolName() throws Exception {
            String body = "{\"toolName\":\"   \",\"arguments\":{}}";

            mockMvc.perform(post("/api/function-call/invoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should accept request with null arguments")
        void shouldAcceptNullArguments() throws Exception {
            when(toolRegistry.invoke(any(ToolInvocation.class)))
                .thenReturn(ToolResult.success("ok"));

            String body = "{\"toolName\": \"rag_search\"}";

            mockMvc.perform(post("/api/function-call/invoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isError").value(false));
        }

        @Test
        @DisplayName("should return structured result when present")
        void shouldReturnStructuredResult() throws Exception {
            Map<String, Object> structured = Map.of("cpu", 45.0, "memory", 60.0);
            ToolResult result = ToolResult.success("metrics", structured);
            when(toolRegistry.invoke(any(ToolInvocation.class))).thenReturn(result);

            String body = "{\"toolName\":\"get_cpu\",\"arguments\":{}}";

            mockMvc.perform(post("/api/function-call/invoke")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.structured.cpu").value(45.0))
                .andExpect(jsonPath("$.structured.memory").value(60.0));
        }
    }
}
