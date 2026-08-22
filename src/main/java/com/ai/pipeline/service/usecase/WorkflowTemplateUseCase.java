package com.ai.pipeline.service.usecase;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.service.WorkflowTemplate;
import java.util.List;

/** Documentation. */
public interface WorkflowTemplateUseCase {
  /** Documentation. */
  List<SavedWorkflowTemplate> listLibrary(String clientId);

  /** Documentation. */
  SavedWorkflowTemplate get(String clientId, String id);

  /** Documentation. */
  SavedWorkflowTemplate create(
      String clientId,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId);

  /** Documentation. */
  SavedWorkflowTemplate update(
      String clientId,
      String id,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt);

  /** Documentation. */
  SavedWorkflowTemplate setEnabled(String clientId, String id, boolean enabled);

  /** Documentation. */
  void delete(String clientId, String id);

  /** Documentation. */
  List<WorkflowTemplate> listTemplates(String language);

  /** Documentation. */
  SavedWorkflowTemplate createFromTemplate(String clientId, String templateId, String language);
}
