package com.ai.vision.web.dto;

import java.util.List;

public record DetectionDto(String class_name, double confidence, List<Double> bbox) {}
