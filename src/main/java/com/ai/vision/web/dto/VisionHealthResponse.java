package com.ai.vision.web.dto;

import java.util.Map;

/** Documentation. */
public record VisionHealthResponse(String status, Map<String, String> providers) {}
