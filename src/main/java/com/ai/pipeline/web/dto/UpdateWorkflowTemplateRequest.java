package com.ai.pipeline.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateWorkflowTemplateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotEmpty List<String> agentTypes,
        @Size(max = 200) String shortTopic,
        @NotBlank String briefPrompt
) {}
