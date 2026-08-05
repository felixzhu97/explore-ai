package com.ai.pipeline.application.usecase;

import com.ai.pipeline.domain.model.SavedAgentDefinition;

import java.util.List;

public interface AgentDefinitionUseCase {

    List<SavedAgentDefinition> listLibrary(String clientId);

    SavedAgentDefinition create(
            String clientId,
            String typeKey,
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys);

    SavedAgentDefinition update(
            String clientId,
            String id,
            String name,
            String description,
            String systemPrompt,
            List<String> toolKeys);

    SavedAgentDefinition setEnabled(String clientId, String id, boolean enabled);

    void delete(String clientId, String id);
}
