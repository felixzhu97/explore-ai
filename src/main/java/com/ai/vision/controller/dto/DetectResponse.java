package com.ai.vision.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record DetectResponse(
    List<DetectionResponse> detections, @JsonAlias("processing_time_ms") long processingTimeMs) {}
