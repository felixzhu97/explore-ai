package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.pipeline.domain.model.SavedWorkflowTemplate;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link SavedWorkflowTemplate}. */
@Repository
public interface SpringDataWorkflowTemplateRepository
    extends JpaRepository<SavedWorkflowTemplate, WorkflowTemplateId> {

  /** Documentation. */
  java.util.Optional<SavedWorkflowTemplate> findByIdAndOwnerKey(
      WorkflowTemplateId id, OwnerKey ownerKey);

  /** Documentation. */
  List<SavedWorkflowTemplate> findAllByOwnerKeyOrderByNameAsc(OwnerKey ownerKey);

  /** Documentation. */
  void deleteByIdAndOwnerKey(WorkflowTemplateId id, OwnerKey ownerKey);

  /** Documentation. */
  boolean existsByOwnerKeyAndName(OwnerKey ownerKey, String name);

  /** Documentation. */
  boolean existsByOwnerKeyAndNameAndIdNot(
      OwnerKey ownerKey, String name, WorkflowTemplateId excludeId);
}
