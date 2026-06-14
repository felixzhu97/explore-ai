package com.ai.interfaces.dto.functioncall;

import com.ai.application.tool.ToolDefinition;

import java.util.Map;

/**
 * Tool definition DTO for API responses.
 *
 * @since 0.2.0
 */
public record ToolDefinitionDto(
    String name,
    String description,
    Map<String, Object> inputSchema,
    boolean composite,
    String category
) {

    /**
     * Factory method to create DTO from domain object.
     */
    public static ToolDefinitionDto from(ToolDefinition def) {
        return new ToolDefinitionDto(
            def.name(),
            def.description(),
            def.inputSchema(),
            def.composite(),
            def.category()
        );
    }
}
