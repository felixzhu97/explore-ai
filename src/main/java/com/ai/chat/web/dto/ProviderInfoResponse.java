package com.ai.chat.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record ProviderInfoResponse(
        String name,
        @JsonAlias("display_name") String displayName,
        List<String> models,
        String status
) {}
