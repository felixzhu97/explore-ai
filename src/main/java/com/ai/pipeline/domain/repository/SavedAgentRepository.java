package com.ai.pipeline.domain.repository;

import com.ai.common.domain.model.OwnerKey;
import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.pipeline.domain.model.SavedAgentId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Persists saved agents scoped by owner, with type keys unique per owner. */
public interface SavedAgentRepository extends Repository<SavedAgent, SavedAgentId> {

  /** Finds the owner's saved agent by id. */
  Optional<SavedAgent> findByIdAndOwnerKey(SavedAgentId id, OwnerKey ownerKey);

  /** Lists the owner's saved agents by name. */
  List<SavedAgent> findAllByOwnerKeyOrderByNameAsc(OwnerKey ownerKey);

  /** Lists the owner's saved agents that are on, by name. */
  List<SavedAgent> findAllByOwnerKeyAndEnabledTrueOrderByNameAsc(OwnerKey ownerKey);

  /** Tells whether the owner already has a saved agent with the type key. */
  boolean existsByOwnerKeyAndTypeKey(OwnerKey ownerKey, String typeKey);

  /** Tells whether another of the owner's saved agents already has the type key. */
  boolean existsByOwnerKeyAndTypeKeyAndIdNot(OwnerKey ownerKey, String typeKey, SavedAgentId id);

  /** Tells whether the owner has a saved agent with the type key other than {@code excludeId}. */
  default boolean existsByOwnerKeyAndTypeKeyIgnoringId(
      OwnerKey ownerKey, String typeKey, SavedAgentId excludeId) {
    return excludeId == null
        ? existsByOwnerKeyAndTypeKey(ownerKey, typeKey)
        : existsByOwnerKeyAndTypeKeyAndIdNot(ownerKey, typeKey, excludeId);
  }

  /** Saves the agent and returns the stored copy. */
  SavedAgent save(SavedAgent agent);

  /** Deletes the owner's saved agent by id. */
  @Transactional
  void deleteByIdAndOwnerKey(SavedAgentId id, OwnerKey ownerKey);
}
