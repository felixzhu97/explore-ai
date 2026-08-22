package com.ai.vision.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Documentation. */
public record CaptionResponse(
    String caption, @JsonAlias("processing_time_ms") long processingTimeMs) {}
