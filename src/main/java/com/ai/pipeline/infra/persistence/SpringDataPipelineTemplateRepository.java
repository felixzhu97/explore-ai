package com.ai.pipeline.infra.persistence;

import com.ai.pipeline.domain.model.PipelineTemplate;
import com.ai.pipeline.domain.model.PipelineTemplateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link PipelineTemplate}. */
@Repository
public interface SpringDataPipelineTemplateRepository
    extends JpaRepository<PipelineTemplate, PipelineTemplateId> {

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByName(String name);

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByNameAndIdNot(String name, PipelineTemplateId excludeId);
}
