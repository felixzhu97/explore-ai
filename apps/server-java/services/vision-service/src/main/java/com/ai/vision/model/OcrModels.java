package com.ai.vision.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OcrRequest(
    boolean includeBboxes,
    String language
) {
    public OcrRequest {
        if (language == null) language = "eng";
    }

    public static OcrRequest defaultConfig() {
        return new OcrRequest(false, "eng");
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OcrResponse(
    String text,
    float confidence,
    List<TextBlock> blocks
) {
    public record TextBlock(
        String text,
        float confidence,
        BoundingBox bbox
    ) {}

    public record BoundingBox(
        float x1,
        float y1,
        float x2,
        float y2
    ) {}
}
