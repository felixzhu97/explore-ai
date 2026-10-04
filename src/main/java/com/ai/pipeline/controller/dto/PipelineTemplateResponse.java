package com.ai.pipeline.controller.dto;

import com.ai.pipeline.domain.model.PipelineTemplate;
import java.time.Instant;
import java.util.List;

public record PipelineTemplateResponse(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String shortTopic,
    String briefPrompt,
    String sourceTemplateId,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {
  /** Builds a response from a user-saved pipeline template. */
  public static PipelineTemplateResponse from(PipelineTemplate template) {
    return new PipelineTemplateResponse(
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
