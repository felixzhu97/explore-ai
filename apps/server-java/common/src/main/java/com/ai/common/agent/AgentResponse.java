package com.ai.common.agent;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentResponse(
	String message,
	AgentType agentType,
	boolean streaming,
	String sessionId,
	List<String> sources,
	String error,
	Map<String, Object> metadata
) {
	public static AgentResponse success(String message, AgentType agentType) {
		return new AgentResponse(message, agentType, false, null, null, null, null);
	}

	public static AgentResponse success(String message, AgentType agentType, String sessionId) {
		return new AgentResponse(message, agentType, false, sessionId, null, null, null);
	}

	public static AgentResponse successWithSources(String message, AgentType agentType, List<String> sources) {
		return new AgentResponse(message, agentType, false, null, sources, null, null);
	}

	public static AgentResponse error(String errorMessage) {
		return new AgentResponse(null, null, false, null, null, errorMessage, null);
	}

	public static AgentResponse streaming(String message, AgentType agentType) {
		return new AgentResponse(message, agentType, true, null, null, null, null);
	}

	public AgentResponse withMetadata(Map<String, Object> newMetadata) {
		return new AgentResponse(message, agentType, streaming, sessionId, sources, error, newMetadata);
	}

	public AgentResponse withRoutedTo(AgentType routedTo) {
		return new AgentResponse(
				message,
				routedTo,
				streaming,
				sessionId,
				sources,
				error,
				Map.of("routedTo", routedTo.name())
		);
	}
}
