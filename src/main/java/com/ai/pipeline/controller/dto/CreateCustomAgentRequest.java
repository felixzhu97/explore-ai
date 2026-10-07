package com.ai.pipeline.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCustomAgentRequest(
    @NotBlank @Size(max = 64) String agentType,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description,
    @NotBlank String systemPrompt,
    List<String> tools) {}
