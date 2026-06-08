package com.ai.common.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentRequest(
	@NotBlank(message = "User message is required")
	String message,

	@NotNull(message = "Agent type is required")
	AgentType agentType,

	String sessionId,

	Integer topK,

	String model,

	Map<String, Object> metadata
) {
	public AgentRequest {
		if (metadata == null) {
			metadata = Map.of();
		}
	}

	public static AgentRequest of(String message, AgentType agentType) {
		return new AgentRequest(message, agentType, null, null, null, null);
	}

	public static AgentRequest of(String message, AgentType agentType, String sessionId) {
		return new AgentRequest(message, agentType, sessionId, null, null, null);
	}
}
