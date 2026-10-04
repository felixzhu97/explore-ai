package com.ai.vision.controller.dto;

import com.ai.common.controller.dto.HealthStatus;

public record VisionHealthResponse(HealthStatus status, VisionProvidersResponse providers) {}
