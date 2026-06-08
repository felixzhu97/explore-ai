package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Default chat agent for general conversational interactions.
 */
@Component
public class ChatAgent implements Agent {

	@Override
	public String name() {
		return "ChatAgent";
	}

	@Override
	public AgentType type() {
		return AgentType.CHAT;
	}

	@Override
	public Mono<AgentResponse> process(AgentRequest request) {
		return Mono.just(AgentResponse.success(
				"Chat response: " + request.message(),
				type(),
				request.sessionId()
		));
	}
}
