package com.ai.mcp.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("McpToolDefinition")
class McpToolDefinitionTest {

  @Test
  @DisplayName("should reject blank tool name")
  void shouldRejectBlankToolName() {
    assertThatThrownBy(() -> McpToolDefinition.create(" ", "desc"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should reject null via compact constructor")
  void shouldRejectNullViaCompactConstructor() {
    assertThatThrownBy(() -> new McpToolDefinition(null, "desc"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
