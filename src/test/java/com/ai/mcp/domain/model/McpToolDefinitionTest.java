package com.ai.mcp.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("McpToolDefinition")
class McpToolDefinitionTest {

    @Test
    @DisplayName("should reject blank tool name")
    void should_reject_blank_tool_name() {
        assertThatThrownBy(() -> McpToolDefinition.create(" ", "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject null via compact constructor")
    void should_reject_null_via_compact_constructor() {
        assertThatThrownBy(() -> new McpToolDefinition(null, "desc"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
