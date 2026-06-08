package com.ai.media.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImageGenerationResponse(
        List<String> images,
        long seed,
        String model,
        String prompt,
        int width,
        int height,
        int numInferenceSteps,
        float guidanceScale,
        double processingTimeMs
) {
    public record StableDiffusionRequest(
            String prompt,
            String negativePrompt,
            int width,
            int height,
            int numInferenceSteps,
            float guidanceScale,
            int seed,
            int batchSize
    ) {
    }

    public record StableDiffusionResponse(
            List<String> images,
            List<Integer> seeds
    ) {
    }
}
