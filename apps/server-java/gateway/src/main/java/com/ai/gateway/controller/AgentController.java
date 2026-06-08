package com.ai.gateway.controller;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.gateway.agent.AgentRegistry;
import com.ai.gateway.agent.SupervisorAgent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

	private static final Logger log = LoggerFactory.getLogger(AgentController.class);

	private final AgentRegistry agentRegistry;
	private final SupervisorAgent supervisorAgent;

	public AgentController(AgentRegistry agentRegistry, SupervisorAgent supervisorAgent) {
		this.agentRegistry = agentRegistry;
		this.supervisorAgent = supervisorAgent;
	}

	@PostMapping("/chat")
	public Mono<ResponseEntity<Map<String, Object>>> chat(@Valid @RequestBody AgentRequest request) {
		log.info("Received chat request for agent type: {}", request.agentType());

		return supervisorAgent.process(request)
				.flatMap(response -> {
					Agent targetAgent = agentRegistry.getAgent(response.agentType())
							.orElse(null);

					if (targetAgent == null || response.agentType() == request.agentType()) {
						return Mono.just(ResponseEntity.ok(Map.<String, Object>of(
								"message", response.message() != null ? response.message() : request.message(),
								"agentType", response.agentType().name()
						)));
					}

					return targetAgent.process(request)
							.<ResponseEntity<Map<String, Object>>>map(result -> ResponseEntity.ok(Map.of(
									"message", result.message() != null ? result.message() : "",
									"agentType", result.agentType().name(),
									"sources", result.sources() != null ? result.sources() : ""
							)))
							.defaultIfEmpty(ResponseEntity.ok(Map.of(
									"message", request.message(),
									"agentType", request.agentType().name()
							)));
				})
				.onErrorResume(e -> {
					log.error("Error processing chat request", e);
					return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
							"error", "Failed to process request: " + e.getMessage()
					)));
				});
	}

	@PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Mono<ResponseEntity<String>> chatStream(@Valid @RequestBody AgentRequest request) {
		log.info("Received streaming chat request for agent type: {}", request.agentType());

		return supervisorAgent.process(request)
				.map(response -> ResponseEntity.ok("data: " + response.message() + "\n\n"))
				.onErrorResume(e -> {
					log.error("Error in streaming chat", e);
					return Mono.just(ResponseEntity.ok("data: [ERROR] " + e.getMessage() + "\n\n"));
				});
	}
}
