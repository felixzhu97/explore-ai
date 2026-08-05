package com.ai.pipeline.domain.repository;

import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.vo.AgentType;

import java.util.List;
import java.util.Optional;

public interface AgentRegistry {

    List<AgentDefinition> listAll();

    List<AgentDefinition> listWorkers();

    Optional<AgentDefinition> findByType(AgentType type);

    AgentDefinition require(AgentType type);
}
