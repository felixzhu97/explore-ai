package com.ai.pipeline.domain.repository;

import com.ai.common.domain.repository.ClientIdOwnedNamedRepository;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;

/** Persists saved workflow templates scoped by client and unique by name. */
public interface WorkflowTemplateRepository
    extends ClientIdOwnedNamedRepository<SavedWorkflowTemplate, WorkflowTemplateId> {}
