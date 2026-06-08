package com.ai.vision.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record DetectionRequest(
    float confidence
) {
    public DetectionRequest {
        if (confidence <= 0) confidence = 0.5f;
        if (confidence > 1) confidence = 1.0f;
    }

    public static DetectionRequest defaultConfig() {
        return new DetectionRequest(0.5f);
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DetectionResponse(
    List<DetectedObject> objects
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
}
