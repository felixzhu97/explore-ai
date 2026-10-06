package com.ai.pipeline.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Agents an owner can use: the built-in catalog, where a saved agent replaces the same type. */
public final class AgentCatalog {

  private AgentCatalog() {}

  /** Merges built-in agents with the owner's saved agents, keeping built-in order first. */
  public static List<AgentDefinition> merge(
      List<AgentDefinition> builtins, List<SavedAgent> library) {
    Map<String, AgentDefinition> byType = new LinkedHashMap<>();
    for (AgentDefinition builtin : builtins) {
      byType.put(builtin.type().value(), builtin);
    }
    for (SavedAgent saved : library) {
      byType.put(saved.getTypeKey(), saved.toAgentDefinition());
    }
    return List.copyOf(byType.values());
  }
}
