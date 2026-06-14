package com.ai.application.tool;

/**
 * Functional interface for tool execution.
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * Executes the tool with the given invocation.
     *
     * @param invocation the tool invocation
     * @return the tool result
     * @throws Exception if execution fails
     */
    ToolResult execute(ToolInvocation invocation) throws Exception;
}
