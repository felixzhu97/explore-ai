package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.vo.SavedAgentId;
import java.util.List;
import java.util.Optional;

/** Documentation. */
public interface SavedAgentRepository {
  /** Documentation. */
  SavedAgentDefinition save(SavedAgentDefinition agent);

  /** Documentation. */
  Optional<SavedAgentDefinition> findByIdAndClientId(SavedAgentId id, String clientId);

  /** Documentation. */
  List<SavedAgentDefinition> findAllByClientId(String clientId);

  /** Documentation. */
  List<SavedAgentDefinition> findEnabledByClientId(String clientId);

  /** Documentation. */
  void deleteByIdAndClientId(SavedAgentId id, String clientId);

  /** Documentation. */
  boolean existsByClientIdAndTypeKeyIgnoringId(
      String clientId, String typeKey, SavedAgentId excludeId);
}
