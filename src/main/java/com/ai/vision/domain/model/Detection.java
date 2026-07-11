package com.ai.vision.domain.model;

public record Detection(
        String className,
        double confidence,
        double x,
        double y,
        double width,
        double height
) {}
