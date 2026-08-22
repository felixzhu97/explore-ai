package com.ai.pipeline.infra.persistence;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link SavedAgentDefinition}. */
@Repository
public interface SpringDataSavedAgentRepository
    extends JpaRepository<SavedAgentDefinition, SavedAgentId> {

  /** Documentation. */
  java.util.Optional<SavedAgentDefinition> findByIdAndOwnerKey(SavedAgentId id, OwnerKey ownerKey);

  /** Documentation. */
  List<SavedAgentDefinition> findAllByOwnerKeyOrderByNameAsc(OwnerKey ownerKey);

  /** Documentation. */
  List<SavedAgentDefinition> findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(OwnerKey ownerKey);

  /** Documentation. */
  void deleteByIdAndOwnerKey(SavedAgentId id, OwnerKey ownerKey);

  /** Documentation. */
  boolean existsByOwnerKeyAndTypeKey(OwnerKey ownerKey, String typeKey);

  /** Documentation. */
  boolean existsByOwnerKeyAndTypeKeyAndIdNot(
      OwnerKey ownerKey, String typeKey, SavedAgentId excludeId);
}
