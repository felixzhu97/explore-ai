package com.ai.pipeline.controller.dto;

import com.ai.pipeline.service.WorkflowTemplate;
import com.ai.pipeline.service.WorkflowTemplateCatalog;
import java.util.List;

/** Documentation. */
public record WorkflowTemplateResponse(
    String id,
    String name,
    String description,
    List<String> agentTypes,
    String shortTopic,
    String briefPrompt,
    List<String> nameAliases) {
  /** Documentation. */
  public static WorkflowTemplateResponse from(WorkflowTemplate template) {
    return new WorkflowTemplateResponse(
        template.id(),
        template.name(),
        template.description(),
        template.agentTypes(),
        template.shortTopic(),
        template.briefPrompt(),
        List.copyOf(WorkflowTemplateCatalog.namesForTemplate(template.id())));
  }
}
