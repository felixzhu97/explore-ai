package com.ai.metrics.controller.dto;

/**
 * Inventory of one metrics domain; the record type follows {@code MetricsDomainResponse.domain}.
 */
public sealed interface DomainInventoryResponse
    permits ChatInventoryResponse,
        RagInventoryResponse,
        AgentsInventoryResponse,
        ToolsInventoryResponse,
        RequestsInventoryResponse {}
