package com.ai.pipeline.domain.repository;

import com.ai.common.domain.repository.OwnerScopedRepository;
import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.vo.PipelineTemplateId;

/** Persists saved pipeline templates scoped by owner and unique by name. */
public interface PipelineTemplateRepository
    extends OwnerScopedRepository<PipelineTemplate, PipelineTemplateId> {}
