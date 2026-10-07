package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentType;
import java.util.List;
import java.util.Optional;

/** Registry of builtin and library agent definitions. */
public interface AgentRegistry {

  /** Builtin agents for the given UI language (display + invoke prompts). */
  List<AgentDefinition> listBuiltins(String language);

  /** Builtin workers + enabled library agents for a client. */
  List<AgentDefinition> listAll(String ownerKey, String language);

  /** Workers eligible for supervisor routing (excludes supervisor and deep). */
  List<AgentDefinition> listWorkers(String ownerKey, String language);

  /** Finds an agent by type. */
  Optional<AgentDefinition> findByType(AgentType type, String ownerKey, String language);

  /** Returns an agent by type, failing when it is missing. */
  AgentDefinition requireAgent(AgentType type, String ownerKey, String language);
}
