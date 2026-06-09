package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoStatusResponse(
    String taskId,
    String status,
    String videoUrl,
    String thumbnailUrl,
    String error,
    Double processingTimeSeconds,
    Map<String, Object> metadata
) {}
