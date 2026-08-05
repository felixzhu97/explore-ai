package com.ai.pipeline.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSavedAgentRequest(
        @NotBlank @Size(max = 64) String typeKey,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotBlank String systemPrompt,
        List<String> toolKeys) {}
