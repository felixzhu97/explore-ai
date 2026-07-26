package com.ai.metrics.domain.repository;

import java.util.Map;

public interface MetricsHealthGateway {

    Map<String, Object> systemStatus();

    Map<String, Object> agentsHealth();

    Map<String, Object> mcpHealth();
}
