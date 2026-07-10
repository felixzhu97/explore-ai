package com.ai.vision.web.dto;

import java.util.List;

public record DetectResponse(List<DetectionDto> detections, long processing_time_ms) {}
