package com.ai.vision.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoGenerateResponse(
    String taskId,
    String status,
    String message,
    String createdAt
) {}
