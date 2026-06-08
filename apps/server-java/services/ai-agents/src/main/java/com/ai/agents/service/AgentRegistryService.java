package com.ai.agents.service;

import com.ai.agents.domain.agent.AiAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry service for managing AI agents.
 * Provides lookup and management of registered agents.
 */
@Service
public class AgentRegistryService {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistryService.class);

    private final Map<String, AiAgent> agentsById = new ConcurrentHashMap<>();
    private final Map<String, AiAgent> agentsByType = new ConcurrentHashMap<>();

    /**
     * Register an agent.
     */
    public void register(AiAgent agent) {
        agentsById.put(agent.getAgentId(), agent);
        agentsByType.put(agent.getAgentType().getId(), agent);
        log.info("Registered agent: {} (type: {})", agent.getAgentId(), agent.getAgentType());
    }

    /**
     * Unregister an agent.
     */
    public void unregister(String agentId) {
        AiAgent agent = agentsById.remove(agentId);
        if (agent != null) {
            agentsByType.remove(agent.getAgentType().getId());
            log.info("Unregistered agent: {}", agentId);
        }
    }

    /**
     * Find an agent by its ID.
     */
    public Optional<AiAgent> findById(String agentId) {
        return Optional.ofNullable(agentsById.get(agentId));
    }

    /**
     * Find an agent by its type ID.
     */
    public Optional<AiAgent> findByType(String typeId) {
        return Optional.ofNullable(agentsByType.get(typeId.toLowerCase()));
    }

    /**
     * Get all registered agents.
     */
    public List<AiAgent> listAll() {
        return List.copyOf(agentsById.values());
    }

    /**
     * Get agent count.
     */
    public int count() {
        return agentsById.size();
    }

    /**
     * Check if an agent is registered.
     */
    public boolean isRegistered(String agentId) {
        return agentsById.containsKey(agentId);
    }
}
