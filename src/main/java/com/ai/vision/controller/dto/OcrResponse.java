package com.ai.vision.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Documentation. */
public record OcrResponse(
    @JsonAlias("full_text") String fullText,
    @JsonAlias("processing_time_ms") long processingTimeMs) {}
