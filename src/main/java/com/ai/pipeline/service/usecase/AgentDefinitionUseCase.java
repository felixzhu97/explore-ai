package com.ai.pipeline.service.usecase;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import java.util.List;

/** Lists, creates, updates, toggles and deletes agent definitions in a client's library. */
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
