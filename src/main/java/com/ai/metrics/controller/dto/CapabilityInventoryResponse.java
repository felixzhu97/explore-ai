package com.ai.metrics.controller.dto;

/**
 * Inventory of one metrics capability; the record type follows {@code
 * MetricsCapabilityResponse.capability}.
 */
public sealed interface CapabilityInventoryResponse
    permits ChatInventoryResponse,
        RagInventoryResponse,
        AgentsInventoryResponse,
        ToolsInventoryResponse,
        RequestsInventoryResponse {}
