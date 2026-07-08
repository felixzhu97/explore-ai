package com.ai.mcp.domain.model;

import com.ai.mcp.domain.exception.McpToolNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("McpToolDefinition")
class McpToolDefinitionTest {

    @Test
    @DisplayName("should reject blank tool name")
    void should_reject_blank_tool_name() {
        assertThatThrownBy(() -> McpToolDefinition.create(" ", "desc"))
                .isInstanceOf(McpToolNotFoundException.class);
    }
}
