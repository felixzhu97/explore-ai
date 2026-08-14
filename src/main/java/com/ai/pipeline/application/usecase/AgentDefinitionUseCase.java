package com.ai.pipeline.application.usecase;

import com.ai.pipeline.domain.model.SavedAgentDefinition;
import java.util.List;

/** Documentation. */
public interface AgentDefinitionUseCase {
  /** Documentation. */
  List<SavedAgentDefinition> listLibrary(String clientId);

  /** Documentation. */
  SavedAgentDefinition create(
      String clientId,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys);

  /** Documentation. */
  SavedAgentDefinition update(
      String clientId,
      String id,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys);

  /** Documentation. */
  SavedAgentDefinition setEnabled(String clientId, String id, boolean enabled);

  /** Documentation. */
  void delete(String clientId, String id);
}
