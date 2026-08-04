package com.ai.agent.domain;

import com.ai.agent.domain.AgentDefinition;
import com.ai.agent.domain.AgentType;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {

    List<AgentDefinition> listAll();

    List<AgentDefinition> listWorkers();

    Optional<AgentDefinition> findByType(AgentType type);

    AgentDefinition require(AgentType type);
}
