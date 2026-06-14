package com.ai.infrastructure.adapter.tool;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;

/**
 * Interface for tool providers.
 * Each tool provider supplies its definition and executor.
 */
public interface ToolProvider {

    /**
     * Returns the tool definition.
     */
    ToolDefinition definition();

    /**
     * Returns the tool executor.
     */
    ToolExecutor executor();
}
