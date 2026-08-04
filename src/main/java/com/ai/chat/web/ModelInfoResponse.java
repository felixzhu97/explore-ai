package com.ai.chat.web;

public record ModelInfoResponse(
        String name,
        String provider,
        String description
) {}
