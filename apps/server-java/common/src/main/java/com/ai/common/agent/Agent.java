package com.ai.common.agent;

import reactor.core.publisher.Mono;

public interface Agent {

	String name();

	AgentType type();

	Mono<AgentResponse> process(AgentRequest request);

	default boolean supports(AgentType type) {
		return this.type() == type;
	}
}
