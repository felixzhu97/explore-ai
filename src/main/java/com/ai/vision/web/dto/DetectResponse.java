package com.ai.vision.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record DetectResponse(List<DetectionDto> detections, @JsonAlias("processing_time_ms") long processingTimeMs) {}
