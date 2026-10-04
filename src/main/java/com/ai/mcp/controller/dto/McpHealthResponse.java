package com.ai.mcp.controller.dto;

import com.ai.common.controller.dto.HealthStatus;

public record McpHealthResponse(
    HealthStatus status, String server, String version, String protocol) {}
