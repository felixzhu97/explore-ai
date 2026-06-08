package com.ai.gateway.controller;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentType;
import com.ai.gateway.agent.AgentRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentHealthController {

	private final AgentRegistry agentRegistry;

	public AgentHealthController(AgentRegistry agentRegistry) {
		this.agentRegistry = agentRegistry;
	}

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> health = new LinkedHashMap<>();
		health.put("status", "UP");
		health.put("timestamp", Instant.now().toString());
		health.put("service", "gateway");

		Map<String, String> agents = new LinkedHashMap<>();
		for (AgentType type : AgentType.values()) {
			agentRegistry.getAgent(type)
					.ifPresent(agent -> agents.put(type.name(), "UP"));
		}
		health.put("agents", agents);

		return ResponseEntity.ok(health);
	}
}
