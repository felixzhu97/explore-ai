package com.ai.agent.infrastructure.registry;

import com.ai.agent.domain.exception.AgentNotFoundException;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.repository.AgentRegistry;
import com.ai.agent.domain.vo.AgentType;
import com.ai.agent.infrastructure.prompt.AgentPromptCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class InMemoryAgentRegistry implements AgentRegistry {

    private final Map<String, AgentDefinition> agents;

    @Autowired
    public InMemoryAgentRegistry(AgentPromptCatalog catalog) {
        Map<String, AgentDefinition> map = new LinkedHashMap<>();
        for (AgentDefinition definition : catalog.defaultAgents()) {
            map.put(definition.type().value(), definition);
        }
        this.agents = Map.copyOf(map);
    }

    /** Test / programmatic construction. */
    public InMemoryAgentRegistry(List<AgentDefinition> definitions) {
        Map<String, AgentDefinition> map = new LinkedHashMap<>();
        for (AgentDefinition definition : definitions) {
            map.put(definition.type().value(), definition);
        }
        this.agents = Map.copyOf(map);
    }

    @Override
    public List<AgentDefinition> listAll() {
        return List.copyOf(agents.values());
    }

    @Override
    public List<AgentDefinition> listWorkers() {
        List<AgentDefinition> workers = new ArrayList<>();
        for (AgentDefinition agent : agents.values()) {
            if (agent.isWorker()) {
                workers.add(agent);
            }
        }
        return List.copyOf(workers);
    }

    @Override
    public Optional<AgentDefinition> findByType(AgentType type) {
        return Optional.ofNullable(agents.get(type.value()));
    }

    @Override
    public AgentDefinition require(AgentType type) {
        return findByType(type).orElseThrow(() -> new AgentNotFoundException(type));
    }
}
