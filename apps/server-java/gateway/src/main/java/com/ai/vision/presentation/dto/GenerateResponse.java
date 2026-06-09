package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateResponse(
    List<String> images,
    int seed,
    String model,
    String prompt,
    int inferenceSteps,
    float guidanceScale,
    int width,
    int height,
    double processingTimeMs
) {
    public static GenerateResponse of(List<String> images, int seed, String model, String prompt,
            int inferenceSteps, float guidanceScale, int width, int height, double processingTimeMs) {
        return new GenerateResponse(images, seed, model, prompt, inferenceSteps, guidanceScale, width, height, processingTimeMs);
    }
}
