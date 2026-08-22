package com.ai.vision.controller.dto;

import java.util.Map;

/** Documentation. */
public record VisionHealthResponse(String status, Map<String, String> providers) {}
