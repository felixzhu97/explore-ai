package com.ai.pipeline.infra.persistence;

import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.pipeline.domain.model.SavedAgentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link SavedAgent}. */
@Repository
public interface SpringDataSavedAgentRepository extends JpaRepository<SavedAgent, SavedAgentId> {

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  List<SavedAgent> findAllByEnabledTrueOrderByNameAsc();

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByTypeKey(String typeKey);

  /** Visible rows are limited by the ownerPartition filter when enabled. */
  boolean existsByTypeKeyAndIdNot(String typeKey, SavedAgentId excludeId);
}
