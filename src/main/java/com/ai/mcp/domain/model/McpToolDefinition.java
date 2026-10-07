package com.ai.mcp.domain.model;

import lombok.Value;

/** Name and description of a tool offered by an MCP server. */
@Value
public class McpToolDefinition {
  String name;
  String description;

  public McpToolDefinition(String name, String description) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("MCP tool name must not be blank");
    }
    name = name.trim();
    description = description != null ? description.trim() : "";
    this.name = name;
    this.description = description;
  }

  /** Creates a tool definition. */
  public static McpToolDefinition createDefinition(String name, String description) {
    return new McpToolDefinition(name, description);
  }
}
