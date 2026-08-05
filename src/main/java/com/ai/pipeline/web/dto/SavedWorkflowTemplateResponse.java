package com.ai.pipeline.web.dto;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;

import java.time.Instant;
import java.util.List;

public record SavedWorkflowTemplateResponse(
        String id,
        String name,
        String description,
        List<String> agentTypes,
        String shortTopic,
        String briefPrompt,
        String sourceTemplateId,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static SavedWorkflowTemplateResponse from(SavedWorkflowTemplate template) {
        return new SavedWorkflowTemplateResponse(
                template.getId().value(),
                template.getName(),
                template.getDescription(),
                template.getAgentTypes(),
                template.getShortTopic(),
                template.getBriefPrompt(),
                template.getSourceTemplateId(),
                template.isEnabled(),
                template.getCreatedAt(),
                template.getUpdatedAt());
    }
}
