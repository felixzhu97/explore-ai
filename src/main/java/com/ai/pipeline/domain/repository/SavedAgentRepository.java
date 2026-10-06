package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import java.util.Optional;

/** Persists saved agents scoped by owner, with type-key uniqueness checks. */
public interface SavedAgentRepository {
  /** Finds the owner's saved agent by id. */
  Optional<SavedAgent> findByIdAndOwnerKey(SavedAgentId id, String ownerKey);

  /** Lists all saved agents of the owner. */
  List<SavedAgent> findAllByOwnerKey(String ownerKey);

  /** Lists the owner's saved agents that are on. */
  List<SavedAgent> findEnabledByOwnerKey(String ownerKey);

  /** Tells whether another saved agent of the owner uses the type key. */
  boolean existsByOwnerKeyAndTypeKeyIgnoringId(
      String ownerKey, String typeKey, SavedAgentId excludeId);

  /** Saves the agent and returns the stored copy. */
  SavedAgent save(SavedAgent agent);

  /** Deletes the owner's saved agent by id. */
  void deleteByIdAndOwnerKey(SavedAgentId id, String ownerKey);
}
