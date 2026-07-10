package com.ai.vision.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record DetectionDto(
        @JsonAlias("class_name") String className,
        double confidence,
        List<Double> bbox
) {}
