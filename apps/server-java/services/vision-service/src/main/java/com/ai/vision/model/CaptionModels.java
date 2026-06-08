package com.ai.vision.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaptionResponse(
    String caption
) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaptionRequest(
    String imageUrl,
    String description
) {}
