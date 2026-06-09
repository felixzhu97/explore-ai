package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpscaleResponse(
    String image,
    int scale,
    int originalWidth,
    int originalHeight,
    int newWidth,
    int newHeight,
    double processingTimeMs
) {}
