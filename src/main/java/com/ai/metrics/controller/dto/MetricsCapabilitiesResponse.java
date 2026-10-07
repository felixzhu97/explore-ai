package com.ai.metrics.controller.dto;

/** Per-capability inventory and health shown on the metrics overview. */
public record MetricsCapabilitiesResponse(
    ChatInventoryResponse chat,
    RagInventoryResponse rag,
    AgentsInventoryResponse agents,
    McpInventoryResponse mcp,
    SystemInventoryResponse system) {}
