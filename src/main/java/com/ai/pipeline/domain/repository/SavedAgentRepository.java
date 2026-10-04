package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import java.util.Optional;

/** Persists saved agent definitions scoped by client, with type-key uniqueness checks. */
public interface SavedAgentRepository {
  SavedAgentDefinition save(SavedAgentDefinition agent);

  Optional<SavedAgentDefinition> findByIdAndOwnerKey(SavedAgentId id, String ownerKey);

  List<SavedAgentDefinition> findAllByOwnerKey(String ownerKey);

  List<SavedAgentDefinition> findEnabledByOwnerKey(String ownerKey);

  void deleteByIdAndOwnerKey(SavedAgentId id, String ownerKey);

  boolean existsByOwnerKeyAndTypeKeyIgnoringId(
      String ownerKey, String typeKey, SavedAgentId excludeId);
}
