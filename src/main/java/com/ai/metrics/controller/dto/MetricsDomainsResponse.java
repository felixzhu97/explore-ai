package com.ai.metrics.controller.dto;

/** Per-domain inventory and health shown on the metrics overview. */
public record MetricsDomainsResponse(
    ChatInventoryResponse chat,
    RagInventoryResponse rag,
    AgentsInventoryResponse agents,
    McpInventoryResponse mcp,
    SystemInventoryResponse system) {}
