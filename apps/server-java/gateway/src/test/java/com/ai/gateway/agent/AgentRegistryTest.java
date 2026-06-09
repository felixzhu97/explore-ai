package com.ai.gateway.agent;

import com.ai.agents.domain.Agent;
import com.ai.agents.domain.AgentCapabilities;
import com.ai.agents.domain.AgentId;
import com.ai.agents.domain.AgentName;
import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.service.AgentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgentRegistry Tests")
class AgentRegistryTest {

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AgentRegistry();
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register agent successfully")
        void shouldRegisterAgentSuccessfully() {
            Agent agent = Agent.create(
                    AgentId.of("chat-1"),
                    AgentName.of("ChatAgent"),
                    AgentType.CHAT,
                    AgentCapabilities.of(AgentType.CHAT)
            );

            registry.register(agent);

            assertThat(registry.hasAgent(AgentType.CHAT)).isTrue();
        }

        @Test
        @DisplayName("should throw exception when registering null agent")
        void shouldThrowExceptionWhenRegisteringNullAgent() {
            assertThatThrownBy(() -> registry.register(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null");
        }

        @Test
        @DisplayName("should register multiple agents")
        void shouldRegisterMultipleAgents() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);
            registry.register("rag", "RagAgent", AgentType.RAG);
            registry.register("supervisor", "SupervisorAgent", AgentType.SUPERVISOR);

            assertThat(registry.size()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("findByType")
    class FindByTypeTests {

        @Test
        @DisplayName("should return agent when found")
        void shouldReturnAgentWhenFound() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);

            Optional<Agent> agent = registry.findByType(AgentType.CHAT);

            assertThat(agent).isPresent();
            assertThat(agent.get().type()).isEqualTo(AgentType.CHAT);
            assertThat(agent.get().name()).isEqualTo(AgentName.of("ChatAgent"));
        }

        @Test
        @DisplayName("should return empty when agent not found")
        void shouldReturnEmptyWhenAgentNotFound() {
            Optional<Agent> agent = registry.findByType(AgentType.TTS);

            assertThat(agent).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasAgent")
    class HasAgentTests {

        @Test
        @DisplayName("should return true when agent exists")
        void shouldReturnTrueWhenAgentExists() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);
            registry.register("rag", "RagAgent", AgentType.RAG);
            registry.register("supervisor", "SupervisorAgent", AgentType.SUPERVISOR);

            assertThat(registry.hasAgent(AgentType.CHAT)).isTrue();
            assertThat(registry.hasAgent(AgentType.RAG)).isTrue();
            assertThat(registry.hasAgent(AgentType.SUPERVISOR)).isTrue();
        }

        @Test
        @DisplayName("should return false when agent does not exist")
        void shouldReturnFalseWhenAgentDoesNotExist() {
            assertThat(registry.hasAgent(AgentType.TTS)).isFalse();
            assertThat(registry.hasAgent(AgentType.VISION)).isFalse();
            assertThat(registry.hasAgent(AgentType.MEDIA)).isFalse();
        }
    }

    @Nested
    @DisplayName("getAllAgents")
    class GetAllAgentsTests {

        @Test
        @DisplayName("should return all registered agents")
        void shouldReturnAllRegisteredAgents() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);
            registry.register("rag", "RagAgent", AgentType.RAG);
            registry.register("supervisor", "SupervisorAgent", AgentType.SUPERVISOR);

            List<Agent> agents = registry.getAllAgents();

            assertThat(agents).hasSize(3);
        }

        @Test
        @DisplayName("should return empty list when no agents registered")
        void shouldReturnEmptyListWhenNoAgentsRegistered() {
            List<Agent> agents = registry.getAllAgents();

            assertThat(agents).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAvailableTypes")
    class GetAvailableTypesTests {

        @Test
        @DisplayName("should return all available types")
        void shouldReturnAllAvailableTypes() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);
            registry.register("rag", "RagAgent", AgentType.RAG);

            Set<AgentType> types = registry.getAvailableTypes();

            assertThat(types).containsExactlyInAnyOrder(AgentType.CHAT, AgentType.RAG);
        }
    }

    @Nested
    @DisplayName("unregister")
    class UnregisterTests {

        @Test
        @DisplayName("should unregister agent by ID")
        void shouldUnregisterAgentById() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);
            AgentId agentId = AgentId.of("chat");

            registry.unregister(agentId);

            assertThat(registry.hasAgent(AgentType.CHAT)).isFalse();
        }
    }

    @Nested
    @DisplayName("size and isEmpty")
    class SizeAndIsEmptyTests {

        @Test
        @DisplayName("should return correct size")
        void shouldReturnCorrectSize() {
            assertThat(registry.size()).isZero();

            registry.register("chat", "ChatAgent", AgentType.CHAT);
            assertThat(registry.size()).isEqualTo(1);

            registry.register("rag", "RagAgent", AgentType.RAG);
            assertThat(registry.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return true when empty")
        void shouldReturnTrueWhenEmpty() {
            assertThat(registry.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("should return false when not empty")
        void shouldReturnFalseWhenNotEmpty() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);

            assertThat(registry.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("clear")
    class ClearTests {

        @Test
        @DisplayName("should clear all agents")
        void shouldClearAllAgents() {
            registry.register("chat", "ChatAgent", AgentType.CHAT);
            registry.register("rag", "RagAgent", AgentType.RAG);

            registry.clear();

            assertThat(registry.isEmpty()).isTrue();
            assertThat(registry.size()).isZero();
        }
    }

    @Nested
    @DisplayName("createDefault")
    class CreateDefaultTests {

        @Test
        @DisplayName("should create default registry with all standard agents")
        void shouldCreateDefaultRegistryWithAllStandardAgents() {
            AgentRegistry defaultRegistry = AgentRegistry.createDefault();

            assertThat(defaultRegistry.size()).isEqualTo(7);
            assertThat(defaultRegistry.hasAgent(AgentType.CHAT)).isTrue();
            assertThat(defaultRegistry.hasAgent(AgentType.RAG)).isTrue();
            assertThat(defaultRegistry.hasAgent(AgentType.TTS)).isTrue();
            assertThat(defaultRegistry.hasAgent(AgentType.VISION)).isTrue();
            assertThat(defaultRegistry.hasAgent(AgentType.MEDIA)).isTrue();
            assertThat(defaultRegistry.hasAgent(AgentType.TEXT)).isTrue();
            assertThat(defaultRegistry.hasAgent(AgentType.SUPERVISOR)).isTrue();
        }
    }

    @Nested
    @DisplayName("constructor with initial agents")
    class ConstructorWithInitialAgentsTests {

        @Test
        @DisplayName("should create registry with initial agents")
        void shouldCreateRegistryWithInitialAgents() {
            List<Agent> initialAgents = List.of(
                    Agent.create(AgentId.of("c1"), AgentName.of("Chat"), AgentType.CHAT, AgentCapabilities.of(AgentType.CHAT)),
                    Agent.create(AgentId.of("r1"), AgentName.of("Rag"), AgentType.RAG, AgentCapabilities.of(AgentType.RAG))
            );

            AgentRegistry registryWithAgents = new AgentRegistry(initialAgents);

            assertThat(registryWithAgents.size()).isEqualTo(2);
            assertThat(registryWithAgents.hasAgent(AgentType.CHAT)).isTrue();
            assertThat(registryWithAgents.hasAgent(AgentType.RAG)).isTrue();
        }

        @Test
        @DisplayName("should handle null initial agents list")
        void shouldHandleNullInitialAgentsList() {
            AgentRegistry registryWithNull = new AgentRegistry(null);

            assertThat(registryWithNull.isEmpty()).isTrue();
        }
    }
}
