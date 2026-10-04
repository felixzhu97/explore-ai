package com.ai.pipeline.service.usecase;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.service.WorkflowTemplate;
import java.util.List;

/** Manages a client's workflow template library and instantiates built-in templates into it. */
public interface WorkflowTemplateUseCase {
  List<SavedWorkflowTemplate> listLibrary(String clientId);

  SavedWorkflowTemplate get(String clientId, String id);

  SavedWorkflowTemplate create(
      String clientId,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId);

  SavedWorkflowTemplate update(
      String clientId,
      String id,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt);

  SavedWorkflowTemplate setEnabled(String clientId, String id, boolean enabled);

  void delete(String clientId, String id);

  List<WorkflowTemplate> listTemplates(String language);

  SavedWorkflowTemplate createFromTemplate(String clientId, String templateId, String language);
}
