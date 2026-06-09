package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaptionResponse(
    String task,
    String model,
    String caption,
    double processingTimeMs
) {
    public static CaptionResponse of(String model, String caption, double processingTimeMs) {
        return new CaptionResponse("caption_image", model, caption, processingTimeMs);
    }
}
