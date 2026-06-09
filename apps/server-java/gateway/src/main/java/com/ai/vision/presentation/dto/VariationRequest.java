package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VariationRequest(
    String image,
    @Size(min = 1, max = 4000) String prompt,
    @Min(0) @Max(1) float strength,
    @Min(1) @Max(150) int numInferenceSteps,
    @Min(1) @Max(20) float guidanceScale,
    Integer seed,
    @Min(1) @Max(4) int numImages
) {
    public VariationRequest {
        if (strength == 0) strength = 0.5f;
        if (numInferenceSteps == 0) numInferenceSteps = 30;
        if (guidanceScale == 0) guidanceScale = 7.5f;
        if (numImages == 0) numImages = 1;
    }
}
