package com.ai.interfaces.dto.functioncall;

import com.ai.application.tool.ToolResult;

import java.util.Map;

/**
 * Tool result DTO for API responses.
 *
 * @since 0.2.0
 */
public record ToolResultDto(
    String content,
    boolean isError,
    Map<String, Object> structured
) {

    /**
     * Factory method to create DTO from domain object.
     */
    public static ToolResultDto from(ToolResult result) {
        return new ToolResultDto(
            result.content(),
            result.isError(),
            result.structured()
        );
    }
}
