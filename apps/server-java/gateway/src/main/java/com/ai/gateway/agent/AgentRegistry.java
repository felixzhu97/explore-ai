package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentRegistry {

	private final Map<AgentType, Agent> agents;

	public AgentRegistry(List<Agent> agentList) {
		this.agents = agentList.stream()
				.collect(Collectors.toMap(
						Agent::type,
						Function.identity(),
						(first, second) -> second
				));
	}

	public Optional<Agent> getAgent(AgentType type) {
		return Optional.ofNullable(agents.get(type));
	}

	public Agent getRequiredAgent(AgentType type) {
		return agents.get(type);
	}

	public Map<AgentType, Agent> getAllAgents() {
		return new ConcurrentHashMap<>(agents);
	}

	public boolean hasAgent(AgentType type) {
		return agents.containsKey(type);
	}
}
