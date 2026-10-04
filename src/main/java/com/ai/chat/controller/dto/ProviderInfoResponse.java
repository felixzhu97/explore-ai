package com.ai.chat.controller.dto;

import java.util.List;

public record ProviderInfoResponse(
    String name, String displayName, List<String> models, String status) {}
