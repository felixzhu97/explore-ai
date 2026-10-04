package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.SavedAgent;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import java.util.Optional;

/** Persists saved agents scoped by owner, with type-key uniqueness checks. */
public interface SavedAgentRepository {
  SavedAgent save(SavedAgent agent);

  Optional<SavedAgent> findByIdAndOwnerKey(SavedAgentId id, String ownerKey);

  List<SavedAgent> findAllByOwnerKey(String ownerKey);

  List<SavedAgent> findEnabledByOwnerKey(String ownerKey);

  void deleteByIdAndOwnerKey(SavedAgentId id, String ownerKey);

  boolean existsByOwnerKeyAndTypeKeyIgnoringId(
      String ownerKey, String typeKey, SavedAgentId excludeId);
}
