package com.ai.vision.web;

import java.util.Map;

public record VisionHealthResponse(String status, Map<String, String> providers) {}
