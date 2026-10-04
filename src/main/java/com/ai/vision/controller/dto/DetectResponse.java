package com.ai.vision.controller.dto;

import java.util.List;

public record DetectResponse(List<DetectionResponse> detections, long processingTimeMs) {}
