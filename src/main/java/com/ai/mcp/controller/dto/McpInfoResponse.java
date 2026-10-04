package com.ai.mcp.controller.dto;

import java.util.Map;

/**
 * MCP server description.
 *
 * @param availableTools tool name to description
 * @param availableResources resource URI template to description
 * @param availablePrompts prompt name to description
 */
public record McpInfoResponse(
    String name,
    String version,
    String description,
    McpCapabilitiesResponse capabilities,
    Map<String, String> availableTools,
    Map<String, String> availableResources,
    Map<String, String> availablePrompts) {}
