package com.ai.vision.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateRequest(
    String prompt,
    String negativePrompt,
    int width,
    int height,
    int steps,
    float guidanceScale
) {
    public GenerateRequest {
        if (width <= 0) width = 512;
        if (height <= 0) height = 512;
        if (steps <= 0) steps = 30;
        if (guidanceScale <= 0) guidanceScale = 7.5f;
        if (negativePrompt == null) negativePrompt = "";
    }

    public static GenerateRequest defaultConfig(String prompt) {
        return new GenerateRequest(prompt, "", 512, 512, 30, 7.5f);
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenerateResponse(
    String imageUrl,
    String base64Image,
    int seed
) {}
