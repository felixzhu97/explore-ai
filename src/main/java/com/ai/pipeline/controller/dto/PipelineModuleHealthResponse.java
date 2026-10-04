package com.ai.pipeline.controller.dto;

import com.ai.common.controller.dto.HealthStatus;

/**
 * Pipeline module health.
 *
 * @param agents number of built-in agents
 */
public record PipelineModuleHealthResponse(HealthStatus status, int agents) {}
