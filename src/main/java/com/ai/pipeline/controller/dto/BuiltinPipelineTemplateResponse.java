package com.ai.pipeline.controller.dto;

import com.ai.pipeline.service.BuiltinPipelineTemplate;
import com.ai.pipeline.service.PipelineTemplateCatalog;
import java.util.List;

public record BuiltinPipelineTemplateResponse(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String shortTopic,
    String briefPrompt,
    List<String> nameAliases) {
  /** Builds a response from a built-in template, adding its names across all languages. */
  public static BuiltinPipelineTemplateResponse from(BuiltinPipelineTemplate template) {
    return new BuiltinPipelineTemplateResponse(
        template.id(),
        template.name(),
        template.description(),
        template.agentTypes(),
        template.shortTopic(),
        template.briefPrompt(),
        List.copyOf(PipelineTemplateCatalog.listNamesForTemplate(template.id())));
  }
}
