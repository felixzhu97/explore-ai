package com.ai.vision.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record CaptionResponse(String caption, @JsonAlias("processing_time_ms") long processingTimeMs) {}
