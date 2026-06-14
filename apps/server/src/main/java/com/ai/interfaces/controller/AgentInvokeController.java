package com.ai.interfaces.controller;

import com.ai.application.port.ToolRegistryPort;
import com.ai.domain.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Agent invoke controller providing SSE streaming endpoints for the 9-tab agent system.
 *
 * <p>The first 7 tabs map to composite tools. The last 2 tabs are:</p>
 * <ul>
 *   <li>{@code mcp} — MCP tools invocation with all available tools</li>
 *   <li>{@code fc} — OpenAI-style Function Call with a single specified tool</li>
 * </ul>
 *
 * @since 0.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
@Tag(name = "Agent", description = "Agent invoke endpoints (9 tabs)")
public class AgentInvokeController {

    private final ToolRegistryPort toolRegistry;
    private final AiChatService aiChatService;

    // Tab name → composite tool name
    private static final Map<String, String> TAB_TOOL_MAP = Map.of(
        "supervisor",  "supervisor_overview",
        "kubernetes",  "k8s_resource_audit",
        "monitoring",  "monitoring_health",
        "model",       "model_evaluation",
        "llmops",      "llmops_pipeline",
        "aiops",       "aiops_anomaly",
        "vector",      "vectordb_inspect"
    );

    // Tab → system prompt
    private static final Map<String, String> TAB_SYSTEM_PROMPTS = Map.of(
        "supervisor",  "You are a system overview assistant. Report the current service health. Prefer using the supervisor_overview tool for comprehensive metrics.",
        "kubernetes",  "You are a K8s resource audit assistant. Output pod schedulability recommendations. Prefer using the k8s_resource_audit tool.",
        "monitoring",  "You are a monitoring assistant. Provide alert thresholds and recommendations. Prefer using the monitoring_health tool.",
        "model",       "You are a model evaluation assistant. Score RAG retrieval quality. Prefer using the model_evaluation tool.",
        "llmops",      "You are a LLM Ops assistant. Analyze session success rates. Prefer using the llmops_pipeline tool.",
        "aiops",       "You are an AIOps assistant. Combine monitoring + external knowledge to locate anomalies. Prefer using the aiops_anomaly tool.",
        "vector",      "You are a vector database administrator. Report corpus size and coverage. Prefer using the vectordb_inspect tool.",
        "mcp",         "You are an MCP tool assistant. You may use all available MCP tools to answer user questions.",
        "fc",          "You are a Function Call assistant. Use the specified tool to answer user questions."
    );

    /**
     * Invoke a composite tool for the given tab with streaming response.
     *
     * @param tab  tab name (supervisor | kubernetes | monitoring | model | llmops | aiops | vector)
     * @param body request body containing message
     * @return SSE emitter streaming the response
     */
    @PostMapping(value = "/{tab}/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Invoke tab agent (SSE)", description = "Streams agent response for a given tab")
    public SseEmitter invokeTab(@PathVariable String tab, @RequestBody Map<String, Object> body) {
        String tool = TAB_TOOL_MAP.get(tab);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tab: " + tab);
        }
        return streamChatResponse(body, tab);
    }

    /**
     * MCP tab: uses all available MCP tools via ChatClient tool callbacks.
     *
     * @param body request body containing message and optional sessionId
     * @return SSE emitter streaming the response
     * @since 0.2.0
     */
    @PostMapping(value = "/mcp/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "MCP agent (SSE)", description = "Streams response using all MCP tools via ChatClient")
    public SseEmitter invokeMcp(@RequestBody Map<String, Object> body) {
        // TODO (P2A): Wire List<ToolCallback> from McpServerConfig for full MCP tool support
        // For now, streams a plain chat response via the default ChatClient
        return streamChatResponse(body, "mcp");
    }

    /**
     * FC tab: uses a single Function Call tool specified in the request.
     *
     * @param body request body containing message, optional toolName, and sessionId
     * @return SSE emitter streaming the response
     * @since 0.2.0
     */
    @PostMapping(value = "/fc/invoke/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Function Call agent (SSE)", description = "Streams response using a single Function Call tool")
    public SseEmitter invokeFc(@RequestBody Map<String, Object> body) {
        // TODO: Implement single-tool FunctionCallDispatcher with auto-execution loop
        // For now, streams a plain chat response via the default ChatClient
        return streamChatResponse(body, "fc");
    }

    private SseEmitter streamChatResponse(Map<String, Object> body, String tab) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        String systemPrompt = TAB_SYSTEM_PROMPTS.getOrDefault(tab, "You are a helpful assistant.");
        String message = getMessage(body);

        try {
            Flux<String> flux = aiChatService.chatStream(message, systemPrompt);

            Consumer<String> onNext = text -> {
                try {
                    emitter.send(SseEmitter.event()
                        .data("{\"type\":\"token\",\"delta\":\"" + escapeJson(text) + "\"}")
                        .build());
                } catch (IOException e) {
                    log.warn("SSE send failed", e);
                }
            };
            flux.subscribe(onNext, emitter::completeWithError, emitter::complete);

        } catch (Exception e) {
            log.error("Error streaming chat response for tab={}", tab, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private String getMessage(Map<String, Object> body) {
        Object msg = body.get("message");
        return msg != null ? msg.toString() : "";
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
