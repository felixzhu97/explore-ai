package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;
import java.util.List;
import java.util.Optional;

/** Registry of builtin and library agent definitions. */
public interface AgentRegistry {

  /** Builtin agents for the given UI language (display + invoke prompts). */
  List<AgentDefinition> listBuiltins(String language);

  /** Builtin workers + enabled library agents for a client. */
  List<AgentDefinition> listAll(String clientId, String language);

  /** Test helper / legacy: builtins in English without client library. */
  default List<AgentDefinition> listAll() {
    return listBuiltins("en");
  }

  /** Workers eligible for supervisor routing (excludes supervisor and deep). */
  List<AgentDefinition> listWorkers(String clientId, String language);

  /** Documentation. */
  default List<AgentDefinition> listWorkers() {
    return listWorkers(null, "en");
  }

  /** Documentation. */
  Optional<AgentDefinition> findByType(AgentType type, String clientId, String language);

  /** Documentation. */
  default Optional<AgentDefinition> findByType(AgentType type) {
    return findByType(type, null, "en");
  }

  /** Documentation. */
  AgentDefinition require(AgentType type, String clientId, String language);

  /** Documentation. */
  default AgentDefinition require(AgentType type) {
    return require(type, null, "en");
  }
}
