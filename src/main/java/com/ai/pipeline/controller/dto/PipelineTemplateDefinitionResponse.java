package com.ai.pipeline.controller.dto;

import com.ai.pipeline.service.PipelineTemplateCatalog;
import com.ai.pipeline.service.PipelineTemplateDefinition;
import java.util.List;

public record PipelineTemplateDefinitionResponse(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String shortTopic,
    String briefPrompt,
    List<String> nameAliases) {
  /** Builds a response from a built-in template, adding its names across all languages. */
  public static PipelineTemplateDefinitionResponse from(PipelineTemplateDefinition template) {
    return new PipelineTemplateDefinitionResponse(
        template.id(),
        template.name(),
        template.description(),
        template.agentTypes(),
        template.shortTopic(),
        template.briefPrompt(),
        List.copyOf(PipelineTemplateCatalog.namesForTemplate(template.id())));
  }
}
