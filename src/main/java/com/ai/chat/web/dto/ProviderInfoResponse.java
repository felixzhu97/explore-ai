package com.ai.chat.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProviderInfoResponse(
        String name,
        @JsonProperty("display_name") String displayName,
        List<String> models,
        String status
) {}
