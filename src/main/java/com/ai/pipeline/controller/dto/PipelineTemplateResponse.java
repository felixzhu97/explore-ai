package com.ai.pipeline.controller.dto;

import com.ai.pipeline.domain.model.PipelineTemplate;
import java.time.Instant;
import java.util.List;

public record PipelineTemplateResponse(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String topic,
    String brief,
    String builtinTemplateId,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt) {
  /** Builds a response from a user-saved pipeline template. */
  public static PipelineTemplateResponse createResponse(PipelineTemplate template) {
    return new PipelineTemplateResponse(
        template.getId().toString(),
        template.getName(),
        template.getDescription(),
        template.getAgentTypes(),
        template.getTopic(),
        template.getBrief(),
        template.getBuiltinTemplateId(),
        template.isEnabled(),
        template.getCreatedAt(),
        template.getUpdatedAt());
  }
}
