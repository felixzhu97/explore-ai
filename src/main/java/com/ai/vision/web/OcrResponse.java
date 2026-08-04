package com.ai.vision.web;

import com.fasterxml.jackson.annotation.JsonAlias;

public record OcrResponse(@JsonAlias("full_text") String fullText, @JsonAlias("processing_time_ms") long processingTimeMs) {}
