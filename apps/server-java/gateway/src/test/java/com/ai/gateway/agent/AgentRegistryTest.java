package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRegistryTest {

	private AgentRegistry registry;

	static class MockAgent implements Agent {
		private final AgentType type;
		private final String name;

		MockAgent(AgentType type, String name) {
			this.type = type;
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public AgentType type() {
			return type;
		}

		@Override
		public Mono<AgentResponse> process(AgentRequest request) {
			return Mono.just(AgentResponse.success("mock response", type));
		}
	}

	@BeforeEach
	void setUp() {
		List<Agent> agents = List.of(
				new MockAgent(AgentType.CHAT, "ChatAgent"),
				new MockAgent(AgentType.RAG, "RagAgent"),
				new MockAgent(AgentType.SUPERVISOR, "SupervisorAgent")
		);
		registry = new AgentRegistry(agents);
	}

	@Nested
	@DisplayName("Get Agent Tests")
	class GetAgentTests {

		@Test
		@DisplayName("Should return agent when found")
		void shouldReturnAgentWhenFound() {
			Optional<Agent> agent = registry.getAgent(AgentType.CHAT);

			assertThat(agent).isPresent();
			assertThat(agent.get().name()).isEqualTo("ChatAgent");
		}

		@Test
		@DisplayName("Should return empty when agent not found")
		void shouldReturnEmptyWhenAgentNotFound() {
			Optional<Agent> agent = registry.getAgent(AgentType.TTS);

			assertThat(agent).isEmpty();
		}

		@Test
		@DisplayName("Should return required agent when found")
		void shouldReturnRequiredAgentWhenFound() {
			Agent agent = registry.getRequiredAgent(AgentType.RAG);

			assertThat(agent).isNotNull();
			assertThat(agent.name()).isEqualTo("RagAgent");
		}

		@Test
		@DisplayName("Should return null when required agent not found")
		void shouldReturnNullWhenRequiredAgentNotFound() {
			Agent agent = registry.getRequiredAgent(AgentType.VISION);

			assertThat(agent).isNull();
		}
	}

	@Nested
	@DisplayName("Has Agent Tests")
	class HasAgentTests {

		@Test
		@DisplayName("Should return true when agent exists")
		void shouldReturnTrueWhenAgentExists() {
			assertThat(registry.hasAgent(AgentType.CHAT)).isTrue();
			assertThat(registry.hasAgent(AgentType.RAG)).isTrue();
			assertThat(registry.hasAgent(AgentType.SUPERVISOR)).isTrue();
		}

		@Test
		@DisplayName("Should return false when agent does not exist")
		void shouldReturnFalseWhenAgentDoesNotExist() {
			assertThat(registry.hasAgent(AgentType.TTS)).isFalse();
			assertThat(registry.hasAgent(AgentType.VISION)).isFalse();
			assertThat(registry.hasAgent(AgentType.MEDIA)).isFalse();
		}
	}

	@Nested
	@DisplayName("Get All Agents Tests")
	class GetAllAgentsTests {

		@Test
		@DisplayName("Should return all registered agents")
		void shouldReturnAllRegisteredAgents() {
			Map<AgentType, Agent> allAgents = registry.getAllAgents();

			assertThat(allAgents).hasSize(3);
			assertThat(allAgents.keySet()).containsExactlyInAnyOrder(
					AgentType.CHAT, AgentType.RAG, AgentType.SUPERVISOR
			);
		}

		@Test
		@DisplayName("Should return copy of agents map")
		void shouldReturnCopyOfAgentsMap() {
			Map<AgentType, Agent> firstCall = registry.getAllAgents();
			Map<AgentType, Agent> secondCall = registry.getAllAgents();

			assertThat(firstCall).isNotSameAs(secondCall);
		}
	}

	@Nested
	@DisplayName("Duplicate Agent Handling Tests")
	class DuplicateAgentHandlingTests {

		@Test
		@DisplayName("Should keep last agent when duplicates registered")
		void shouldKeepLastAgentWhenDuplicatesRegistered() {
			List<Agent> agentsWithDuplicates = List.of(
					new MockAgent(AgentType.CHAT, "FirstChatAgent"),
					new MockAgent(AgentType.CHAT, "SecondChatAgent")
			);
			AgentRegistry registryWithDuplicates = new AgentRegistry(agentsWithDuplicates);

			Agent chatAgent = registryWithDuplicates.getRequiredAgent(AgentType.CHAT);

			assertThat(chatAgent.name()).isEqualTo("SecondChatAgent");
		}
	}

	@Nested
	@DisplayName("Agent Process Tests")
	class AgentProcessTests {

		@Test
		@DisplayName("Should process request through agent")
		void shouldProcessRequestThroughAgent() {
			Agent chatAgent = registry.getRequiredAgent(AgentType.CHAT);
			AgentRequest request = AgentRequest.of("Hello", AgentType.CHAT);

			AgentResponse response = chatAgent.process(request).block();

			assertThat(response).isNotNull();
			assertThat(response.message()).isEqualTo("mock response");
			assertThat(response.agentType()).isEqualTo(AgentType.CHAT);
		}
	}
}
