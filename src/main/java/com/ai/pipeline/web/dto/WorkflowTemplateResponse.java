package com.ai.pipeline.web.dto;

import com.ai.pipeline.application.WorkflowTemplate;
import com.ai.pipeline.application.WorkflowTemplateCatalog;

import java.util.List;

public record WorkflowTemplateResponse(
        String id,
        String name,
        String description,
        List<String> agentTypes,
        String shortTopic,
        String briefPrompt,
        List<String> nameAliases
) {
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
