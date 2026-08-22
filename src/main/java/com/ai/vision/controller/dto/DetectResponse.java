package com.ai.vision.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/** Documentation. */
public record DetectResponse(
    List<DetectionDto> detections, @JsonAlias("processing_time_ms") long processingTimeMs) {}
