package com.ai.infrastructure.adapter.tool;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory tool registry that collects all ToolProvider beans.
 */
@Component
public class InMemoryToolRegistry implements ToolRegistryPort {

    private final Map<String, RegisteredTool> registry = new ConcurrentHashMap<>();

    public InMemoryToolRegistry(List<ToolProvider> toolProviders) {
        for (ToolProvider provider : toolProviders) {
            ToolDefinition def = provider.definition();
            if (def == null) {
                continue;
            }
            String name = def.name();
            if (name == null || name.isBlank()) {
                continue;
            }
            registry.put(name, new RegisteredTool(def, provider.executor()));
        }
    }

    @Override
    public List<ToolDefinition> listTools() {
        return new ArrayList<>(registry.values().stream()
            .map(rt -> rt.def)
            .collect(Collectors.toList()));
    }

    @Override
    public Optional<ToolDefinition> findByName(String name) {
        RegisteredTool tool = registry.get(name);
        return tool != null ? Optional.of(tool.def) : Optional.empty();
    }

    @Override
    public ToolResult invoke(ToolInvocation invocation) {
        RegisteredTool tool = registry.get(invocation.toolName());
        if (tool == null) {
            return ToolResult.error("Tool not found: " + invocation.toolName());
        }
        try {
            return tool.exec().execute(invocation);
        } catch (Exception e) {
            return ToolResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    private record RegisteredTool(ToolDefinition def, ToolExecutor exec) {}
}
