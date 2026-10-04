package com.ai.pipeline.infra.persistence;

import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link SavedWorkflowTemplate}. */
@Repository
public interface SpringDataWorkflowTemplateRepository
    extends JpaRepository<SavedWorkflowTemplate, WorkflowTemplateId> {

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByName(String name);

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByNameAndIdNot(String name, WorkflowTemplateId excludeId);
}
