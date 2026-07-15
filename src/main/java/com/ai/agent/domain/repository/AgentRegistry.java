package com.ai.agent.domain.repository;

import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.vo.AgentType;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {

    List<AgentDefinition> listAll();

    List<AgentDefinition> listWorkers();

    Optional<AgentDefinition> findByType(AgentType type);

    AgentDefinition require(AgentType type);
}
