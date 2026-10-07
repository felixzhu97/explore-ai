package com.ai.metrics.service.model;

import com.ai.metrics.domain.model.ModuleStatus;
import com.ai.metrics.domain.repository.MetricsHealthGateway.AgentsHealth;
import com.ai.metrics.domain.repository.MetricsHealthGateway.McpHealth;
import com.ai.metrics.domain.repository.MetricsQueryRepository.ChatInventory;
import com.ai.metrics.domain.repository.MetricsQueryRepository.RagInventory;

/** Per-capability inventory and health shown on the metrics overview. */
public record OverviewCapabilities(
    ChatInventory chat,
    RagInventory rag,
    AgentsHealth agents,
    McpHealth mcp,
    ModuleStatus system) {}
