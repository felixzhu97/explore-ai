package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetectionResponse(
    String task,
    String model,
    List<DetectedObject> objects,
    int imageWidth,
    int imageHeight,
    double processingTimeMs
) {
    public record DetectedObject(
        String label,
        float confidence,
        BoundingBox bbox
    ) {}

    public record BoundingBox(
        float x,
        float y,
        float width,
        float height
    ) {}

    public static DetectionResponse empty(String model, int width, int height) {
        return new DetectionResponse("detect_objects", model, List.of(), width, height, 0.0);
    }

    public static DetectionResponse of(String model, List<DetectedObject> objects, int width, int height, double processingTimeMs) {
        return new DetectionResponse("detect_objects", model, objects, width, height, processingTimeMs);
    }
}
