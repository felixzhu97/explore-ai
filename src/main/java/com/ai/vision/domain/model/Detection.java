package com.ai.vision.domain.model;

/** Documentation. */
public record Detection(
    String className, double confidence, double x, double y, double width, double height) {}
