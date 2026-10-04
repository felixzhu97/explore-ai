package com.ai.vision.controller.dto;

import com.ai.common.controller.dto.HealthStatus;

/** Availability of each vision provider. */
public record VisionProvidersResponse(
    HealthStatus caption, HealthStatus detect, HealthStatus ocr) {}
