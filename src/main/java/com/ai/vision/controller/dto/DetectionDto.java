package com.ai.vision.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

/** Documentation. */
public record DetectionDto(
    @JsonAlias("class_name") String className, double confidence, List<Double> bbox) {}
