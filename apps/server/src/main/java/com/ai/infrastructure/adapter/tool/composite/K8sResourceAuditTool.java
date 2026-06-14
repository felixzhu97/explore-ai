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
 * Composite tool: Kubernetes resource audit from local perspective.
 */
@Component
public class K8sResourceAuditTool implements ToolProvider {

    private final ToolRegistryPort registry;
    private final CompositeToolSupport support;

    public K8sResourceAuditTool(@Lazy ToolRegistryPort registry) {
        this.registry = registry;
        this.support = new CompositeToolSupport(registry);
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.integerProp("topK", "Number of top results (default 5)", false)
        );
        return ToolDefinition.composite(
            "k8s_resource_audit",
            "K8s resource audit: retrieves CPU, memory, disk, load metrics and searches for K8s best practices.",
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
            List<SubToolCall> calls = List.of(
                new SubToolCall("get_cpu", Map.of()),
                new SubToolCall("get_memory", Map.of()),
                new SubToolCall("get_disk", Map.of()),
                new SubToolCall("get_load", Map.of()),
                new SubToolCall("rag_search", Map.of("query", "kubernetes best practices", "topK", 3))
            );

            CompositeResult result = support.executeAll(calls);

            String md = "# K8s Resource Audit\n\n" + result.markdown();
            return ToolResult.success(md, result.structured());
        } catch (Exception e) {
            return ToolResult.error("k8s_resource_audit failed: " + e.getMessage());
        }
    }
}
