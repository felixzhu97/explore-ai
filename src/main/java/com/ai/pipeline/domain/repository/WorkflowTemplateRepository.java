package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import java.util.List;
import java.util.Optional;

/** Documentation. */
public interface WorkflowTemplateRepository {
  /** Documentation. */
  SavedWorkflowTemplate save(SavedWorkflowTemplate template);

  /** Documentation. */
  Optional<SavedWorkflowTemplate> findByIdAndClientId(WorkflowTemplateId id, String clientId);

  /** Documentation. */
  List<SavedWorkflowTemplate> findAllByClientId(String clientId);

  /** Documentation. */
  void deleteByIdAndClientId(WorkflowTemplateId id, String clientId);

  /** Documentation. */
  boolean existsByClientIdAndNameIgnoringId(
      String clientId, String name, WorkflowTemplateId excludeId);
}
