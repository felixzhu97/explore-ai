package com.ai.infrastructure.adapter.mcp;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

import java.util.Map;

/**
 * Adapter bridging our application-layer tool abstraction ({@link ToolDefinition} /
 * {@link ToolRegistryPort}) to Spring AI 2.0's {@link ToolCallback} interface.
 *
 * <p>The MCP server starter auto-collects all {@link ToolCallback} beans and exposes
 * them as JSON-RPC tools at the configured endpoint.
 */
public class McpToolCallbackAdapter implements ToolCallback {

    private final ToolDefinition toolDefinition;
    private final ToolRegistryPort registry;
    private final ObjectMapper objectMapper;

    McpToolCallbackAdapter(ToolDefinition toolDefinition,
                           ToolRegistryPort registry,
                           ObjectMapper objectMapper) {
        this.toolDefinition = toolDefinition;
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a callback that uses a default {@link ObjectMapper}.
     */
    public McpToolCallbackAdapter(ToolDefinition toolDefinition, ToolRegistryPort registry) {
        this(toolDefinition, registry, new ObjectMapper());
    }

    @Override
    public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
        String inputSchemaJson = serializeSchema(toolDefinition.inputSchema());
        return org.springframework.ai.tool.definition.ToolDefinition.builder()
                .name(toolDefinition.name())
                .description(toolDefinition.description())
                .inputSchema(inputSchemaJson)
                .build();
    }

    /**
     * Executes the tool by parsing the JSON input into arguments and delegating to
     * {@link ToolRegistryPort#invoke}.
     *
     * @param toolInput JSON string of arguments, e.g. {@code "{\"query\":\"x\"}"}
     * @return the tool result content as a string
     * @throws ToolExecutionException if the tool is not found or execution fails
     */
    @Override
    public String call(String toolInput) {
        Map<String, Object> args = parseArgs(toolInput);
        ToolInvocation invocation = new ToolInvocation(toolDefinition.name(), args);

        ToolResult result = registry.invoke(invocation);

        if (result.isError()) {
            throw new ToolExecutionException(
                    "Tool '" + toolDefinition.name() + "' error: " + result.content()
            );
        }

        return result.content();
    }

    /**
     * Delegates to {@link #call(String)} ignoring the context parameter.
     */
    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }

    private Map<String, Object> parseArgs(String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(toolInput, Map.class);
        } catch (JsonProcessingException e) {
            throw new ToolExecutionException(
                    "Failed to parse tool input JSON: " + e.getMessage()
            );
        }
    }

    private String serializeSchema(Map<String, Object> schema) {
        if (schema == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to serialize tool input schema for '" + toolDefinition.name() + "'", e
            );
        }
    }

    /**
     * Exception thrown when tool execution fails.
     */
    public static class ToolExecutionException extends RuntimeException {
        public ToolExecutionException(String message) {
            super(message);
        }

        public ToolExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
