package com.ai.chat.web.dto;

public record ModelInfoResponse(
        String name,
        String provider,
        String description
) {}
