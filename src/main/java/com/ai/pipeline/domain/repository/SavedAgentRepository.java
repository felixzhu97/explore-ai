package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import com.ai.pipeline.domain.vo.SavedAgentId;

import java.util.List;
import java.util.Optional;

public interface SavedAgentRepository {

    SavedAgentDefinition save(SavedAgentDefinition agent);

    Optional<SavedAgentDefinition> findByIdAndClientId(SavedAgentId id, String clientId);

    List<SavedAgentDefinition> findAllByClientId(String clientId);

    List<SavedAgentDefinition> findEnabledByClientId(String clientId);

    void deleteByIdAndClientId(SavedAgentId id, String clientId);

    boolean existsByClientIdAndTypeKeyIgnoringId(String clientId, String typeKey, SavedAgentId excludeId);
}
