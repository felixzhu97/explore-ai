package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VariationResponse(
    List<String> images,
    int seed,
    String prompt,
    float strength,
    int inferenceSteps,
    double processingTimeMs
) {}
