package com.ai.pipeline.domain.repository;

import com.ai.common.domain.repository.ClientIdOwnedNamedRepository;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;

/** Documentation. */
public interface WorkflowTemplateRepository
    extends ClientIdOwnedNamedRepository<SavedWorkflowTemplate, WorkflowTemplateId> {}
