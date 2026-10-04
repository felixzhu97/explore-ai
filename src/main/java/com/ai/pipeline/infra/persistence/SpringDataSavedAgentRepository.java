package com.ai.pipeline.infra.persistence;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link SavedAgentDefinition}. */
@Repository
public interface SpringDataSavedAgentRepository
    extends JpaRepository<SavedAgentDefinition, SavedAgentId> {

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  List<SavedAgentDefinition> findAllByEnabledTrueOrderByNameAsc();

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByTypeKey(String typeKey);

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByTypeKeyAndIdNot(String typeKey, SavedAgentId excludeId);
}
