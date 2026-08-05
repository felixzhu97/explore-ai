package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;

import java.util.List;
import java.util.Optional;

public interface WorkflowTemplateRepository {

    SavedWorkflowTemplate save(SavedWorkflowTemplate template);

    Optional<SavedWorkflowTemplate> findByIdAndClientId(WorkflowTemplateId id, String clientId);

    List<SavedWorkflowTemplate> findAllByClientId(String clientId);

    void deleteByIdAndClientId(WorkflowTemplateId id, String clientId);

    boolean existsByClientIdAndNameIgnoringId(String clientId, String name, WorkflowTemplateId excludeId);
}
