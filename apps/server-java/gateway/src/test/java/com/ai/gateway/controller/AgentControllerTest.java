package com.ai.gateway.controller;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import com.ai.gateway.agent.AgentRegistry;
import com.ai.gateway.agent.SupervisorAgent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {AgentController.class, AgentHealthController.class})
class AgentControllerTest {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private AgentRegistry agentRegistry;

	@MockBean
	private SupervisorAgent supervisorAgent;

	static class MockAgent implements Agent {
		private final AgentType type;

		MockAgent(AgentType type) {
			this.type = type;
		}

		@Override
		public String name() {
			return type.name() + "Agent";
		}

		@Override
		public AgentType type() {
			return type;
		}

		@Override
		public Mono<AgentResponse> process(AgentRequest request) {
			return Mono.just(AgentResponse.success("Response from " + type.name(), type));
		}
	}

	@Nested
	@DisplayName("POST /api/agents/chat Tests")
	class ChatEndpointTests {

		@Test
		@DisplayName("Should return chat response successfully")
		void shouldReturnChatResponseSuccessfully() throws Exception {
			AgentRequest request = new AgentRequest(
					"Hello",
					AgentType.CHAT,
					"session-123",
					null,
					null,
					null
			);

			when(supervisorAgent.process(any(AgentRequest.class)))
					.thenReturn(Mono.just(AgentResponse.success("Hello", AgentType.CHAT)));

			when(agentRegistry.getAgent(AgentType.CHAT))
					.thenReturn(Optional.of(new MockAgent(AgentType.CHAT)));

			webTestClient.post()
					.uri("/api/agents/chat")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(objectMapper.writeValueAsString(request))
					.exchange()
					.expectStatus().isOk()
					.expectBody()
					.jsonPath("$.message").isEqualTo("Response from CHAT")
					.jsonPath("$.agentType").isEqualTo("CHAT");
		}

		@Test
		@DisplayName("Should handle supervisor routing to RAG agent")
		void shouldHandleSupervisorRoutingToRagAgent() throws Exception {
			AgentRequest request = new AgentRequest(
					"search documents",
					AgentType.SUPERVISOR,
					"session-456",
					null,
					null,
					null
			);

			when(supervisorAgent.process(any(AgentRequest.class)))
					.thenReturn(Mono.just(AgentResponse.success("search documents", AgentType.RAG)));

			when(agentRegistry.getAgent(AgentType.RAG))
					.thenReturn(Optional.of(new MockAgent(AgentType.RAG)));

			webTestClient.post()
					.uri("/api/agents/chat")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(objectMapper.writeValueAsString(request))
					.exchange()
					.expectStatus().isOk()
					.expectBody()
					.jsonPath("$.message").isEqualTo("Response from RAG")
					.jsonPath("$.agentType").isEqualTo("RAG");
		}

		@Test
		@DisplayName("Should return error response on exception")
		void shouldReturnErrorResponseOnException() throws Exception {
			AgentRequest request = new AgentRequest(
					"test",
					AgentType.CHAT,
					null,
					null,
					null,
					null
			);

			when(supervisorAgent.process(any(AgentRequest.class)))
					.thenReturn(Mono.error(new RuntimeException("Test error")));

			webTestClient.post()
					.uri("/api/agents/chat")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(objectMapper.writeValueAsString(request))
					.exchange()
					.expectStatus().is5xxServerError()
					.expectBody()
					.jsonPath("$.error").exists();
		}

		@Test
		@DisplayName("Should validate required message field")
		void shouldValidateRequiredMessageField() {
			String invalidRequest = """
					{
						"agentType": "CHAT"
					}
					""";

			webTestClient.post()
					.uri("/api/agents/chat")
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(invalidRequest)
					.exchange()
					.expectStatus().isBadRequest();
		}
	}

	@Nested
	@DisplayName("GET /api/agents/health Tests")
	class HealthEndpointTests {

		@Test
		@DisplayName("Should return health status with agents")
		void shouldReturnHealthStatusWithAgents() {
			when(agentRegistry.getAgent(AgentType.CHAT))
					.thenReturn(Optional.of(new MockAgent(AgentType.CHAT)));
			when(agentRegistry.getAgent(AgentType.SUPERVISOR))
					.thenReturn(Optional.of(new MockAgent(AgentType.SUPERVISOR)));

			webTestClient.get()
					.uri("/api/agents/health")
					.exchange()
					.expectStatus().isOk()
					.expectBody()
					.jsonPath("$.status").isEqualTo("UP")
					.jsonPath("$.service").isEqualTo("gateway")
					.jsonPath("$.agents.CHAT").isEqualTo("UP")
					.jsonPath("$.agents.SUPERVISOR").isEqualTo("UP")
					.jsonPath("$.timestamp").exists();
		}
	}
}
