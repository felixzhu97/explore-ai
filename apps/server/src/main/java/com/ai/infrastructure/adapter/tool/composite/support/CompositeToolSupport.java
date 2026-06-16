package com.ai.infrastructure.adapter.tool.composite.support;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;

import java.util.*;

/**
 * Helper for executing sub-tool calls in composite tools.
 */
public class CompositeToolSupport {

    private static final Logger log = LoggerFactory.getLogger(CompositeToolSupport.class);

    private final ToolRegistryPort registry;

    public CompositeToolSupport(@Lazy ToolRegistryPort registry) {
        this.registry = registry;
    }

    /**
     * Executes a list of sub-tool calls and aggregates results.
     */
    public CompositeResult executeAll(List<SubToolCall> calls) {
        Map<String, ToolResult> results = new LinkedHashMap<>();
        StringBuilder markdown = new StringBuilder();
        Map<String, Object> structured = new LinkedHashMap<>();

        for (SubToolCall call : calls) {
            try {
                ToolInvocation inv = new ToolInvocation(call.name, call.args);
                ToolResult result = registry.invoke(inv);

                results.put(call.name, result);
                structured.put(call.name, result.structured() != null ? result.structured() : Map.of("content", result.content()));

                if (result.isError()) {
                    markdown.append("### `").append(call.name).append("` ⚠️\n")
                        .append("> Error: ").append(result.content()).append("\n\n");
                } else {
                    markdown.append("### `").append(call.name).append("`\n")
                        .append(result.content()).append("\n\n");
                }
            } catch (Exception e) {
                log.error("Composite tool sub-call failed: {}", call.name, e);
                results.put(call.name, ToolResult.error("Sub-call failed: " + e.getMessage()));
                structured.put(call.name, Map.of("error", e.getMessage()));
                markdown.append("### `").append(call.name).append("` ❌\n")
                    .append("> Execution failed: ").append(e.getMessage()).append("\n\n");
            }
        }

        return new CompositeResult(markdown.toString(), structured);
    }

    public record SubToolCall(String name, Map<String, Object> args) {}

    public record CompositeResult(String markdown, Map<String, Object> structured) {}
}
