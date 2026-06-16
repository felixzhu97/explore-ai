package com.ai.infrastructure.adapter.tool.composite;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import com.ai.infrastructure.adapter.tool.JsonSchemaBuilder;
import com.ai.infrastructure.adapter.tool.composite.support.CompositeToolSupport;
import com.ai.infrastructure.adapter.tool.composite.support.CompositeToolSupport.CompositeResult;
import com.ai.infrastructure.adapter.tool.composite.support.CompositeToolSupport.SubToolCall;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Composite tool: AIOps anomaly detection combining monitoring + RAG.
 */
@Component
public class AiopSAnomalyTool implements ToolProvider {

    private final ToolRegistryPort registry;
    private final CompositeToolSupport support;

    public AiopSAnomalyTool(@Lazy ToolRegistryPort registry) {
        this.registry = registry;
        this.support = new CompositeToolSupport(registry);
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.integerProp("topK", "Number of top results (default 5)", false),
            JsonSchemaBuilder.stringProp("query", "Custom query for anomaly detection (default: AIOps anomaly detection)", false)
        );
        return ToolDefinition.composite(
            "aiops_anomaly",
            "AIOps anomaly detection: retrieves CPU, memory, load metrics and searches for anomaly detection knowledge.",
            JsonSchemaBuilder.objectSchema(List.of(), props),
            "composite"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        try {
            String query = invocation.getArg("query", "AIOps anomaly detection");
            int topK = invocation.getArg("topK", 5);

            List<SubToolCall> calls = List.of(
                new SubToolCall("get_cpu", Map.of()),
                new SubToolCall("get_memory", Map.of()),
                new SubToolCall("get_load", Map.of()),
                new SubToolCall("rag_search", Map.of("query", query, "topK", topK))
            );

            CompositeResult result = support.executeAll(calls);

            String md = "# AIOps Anomaly Detection\n\n" + result.markdown();
            return ToolResult.success(md, result.structured());
        } catch (Exception e) {
            return ToolResult.error("aiops_anomaly failed: " + e.getMessage());
        }
    }
}
