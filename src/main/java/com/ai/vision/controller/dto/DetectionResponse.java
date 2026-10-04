package com.ai.vision.controller.dto;

import java.util.List;

public record DetectionResponse(String className, double confidence, List<Double> bbox) {}
