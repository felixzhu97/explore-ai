package com.ai.metrics.controller.dto;

import java.time.Instant;

public record InvocationEventResponse(
    String id,
    Instant occurredAt,
    MetricsCapability capability,
    String operation,
    MetricsOutcome outcome,
    long latencyMs,
    String provider,
    String model,
    String sessionId,
    String documentId,
    String agentType,
    String toolName,
    Integer promptTokens,
    Integer completionTokens,
    String errorCode,
    String errorMessage) {}
