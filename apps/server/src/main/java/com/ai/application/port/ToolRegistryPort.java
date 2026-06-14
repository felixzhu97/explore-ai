package com.ai.application.port;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;

import java.util.List;
import java.util.Optional;

/**
 * Registry port for tool discovery and invocation.
 * Defines the contract for registering and executing tools.
 */
public interface ToolRegistryPort {

    /**
     * Lists all registered tools.
     */
    List<ToolDefinition> listTools();

    /**
     * Finds a tool definition by name.
     */
    Optional<ToolDefinition> findByName(String name);

    /**
     * Invokes a tool by name with the given arguments.
     */
    ToolResult invoke(ToolInvocation invocation);
}
