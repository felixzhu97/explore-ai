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
 * Composite tool: LLM Ops pipeline diagnostics.
 */
@Component
public class LlmoopsPipelineTool implements ToolProvider {

    private final ToolRegistryPort registry;
    private final CompositeToolSupport support;

    public LlmoopsPipelineTool(@Lazy ToolRegistryPort registry) {
        this.registry = registry;
        this.support = new CompositeToolSupport(registry);
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.integerProp("topK", "Number of top results (default 5)", false)
        );
        return ToolDefinition.composite(
            "llmops_pipeline",
            "LLM Ops pipeline diagnostics: searches for LLMOps pipeline best practices and retrieves JVM + memory metrics.",
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
            int topK = invocation.getArg("topK", 5);

            List<SubToolCall> calls = List.of(
                new SubToolCall("rag_search", Map.of("query", "LLMOps pipeline", "topK", topK)),
                new SubToolCall("get_jvm", Map.of()),
                new SubToolCall("get_memory", Map.of())
            );

            CompositeResult result = support.executeAll(calls);

            String md = "# LLMOps Pipeline\n\n" + result.markdown();
            return ToolResult.success(md, result.structured());
        } catch (Exception e) {
            return ToolResult.error("llmops_pipeline failed: " + e.getMessage());
        }
    }
}
