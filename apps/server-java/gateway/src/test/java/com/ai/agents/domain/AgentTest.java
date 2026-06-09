package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Agent Tests")
class AgentTest {

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create agent with all parameters")
        void shouldCreateAgentWithAllParameters() {
            AgentId id = AgentId.of("agent-001");
            AgentName name = AgentName.of("TestAgent");
            AgentType type = AgentType.CHAT;
            AgentCapabilities capabilities = AgentCapabilities.of(type);
            String description = "Test description";

            Agent agent = Agent.create(id, name, type, capabilities, description);

            assertThat(agent.id()).isEqualTo(id);
            assertThat(agent.name()).isEqualTo(name);
            assertThat(agent.type()).isEqualTo(type);
            assertThat(agent.capabilities()).isEqualTo(capabilities);
            assertThat(agent.description()).isEqualTo(description);
        }

        @Test
        @DisplayName("should create agent without description")
        void shouldCreateAgentWithoutDescription() {
            AgentId id = AgentId.of("agent-001");
            AgentName name = AgentName.of("TestAgent");
            AgentType type = AgentType.RAG;
            AgentCapabilities capabilities = AgentCapabilities.of(type);

            Agent agent = Agent.create(id, name, type, capabilities);

            assertThat(agent.description()).isEmpty();
        }

        @Test
        @DisplayName("should throw exception when id is null")
        void shouldThrowExceptionWhenIdIsNull() {
            AgentName name = AgentName.of("TestAgent");
            AgentType type = AgentType.CHAT;
            AgentCapabilities capabilities = AgentCapabilities.of(type);

            assertThatThrownBy(() -> Agent.create(null, name, type, capabilities))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Required parameters cannot be null");
        }

        @Test
        @DisplayName("should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            AgentId id = AgentId.of("agent-001");
            AgentType type = AgentType.CHAT;
            AgentCapabilities capabilities = AgentCapabilities.of(type);

            assertThatThrownBy(() -> Agent.create(id, null, type, capabilities))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception when type is null")
        void shouldThrowExceptionWhenTypeIsNull() {
            AgentId id = AgentId.of("agent-001");
            AgentName name = AgentName.of("TestAgent");
            AgentCapabilities capabilities = AgentCapabilities.of(AgentType.CHAT);

            assertThatThrownBy(() -> Agent.create(id, name, null, capabilities))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw exception when capabilities is none")
        void shouldThrowExceptionWhenCapabilitiesIsNone() {
            AgentId id = AgentId.of("agent-001");
            AgentName name = AgentName.of("TestAgent");
            AgentType type = AgentType.CHAT;

            assertThatThrownBy(() -> Agent.create(id, name, type, AgentCapabilities.none()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must have at least one capability");
        }
    }

    @Nested
    @DisplayName("convenience factory methods")
    class ConvenienceFactoryMethodsTests {

        @Test
        @DisplayName("should create chat agent")
        void shouldCreateChatAgent() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");

            assertThat(agent.type()).isEqualTo(AgentType.CHAT);
            assertThat(agent.capabilities().supportsChat()).isTrue();
        }

        @Test
        @DisplayName("should create RAG agent")
        void shouldCreateRagAgent() {
            Agent agent = Agent.ragAgent("rag-1", "RagAgent");

            assertThat(agent.type()).isEqualTo(AgentType.RAG);
            assertThat(agent.capabilities().supportsRag()).isTrue();
        }

        @Test
        @DisplayName("should create TTS agent")
        void shouldCreateTtsAgent() {
            Agent agent = Agent.ttsAgent("tts-1", "TtsAgent");

            assertThat(agent.type()).isEqualTo(AgentType.TTS);
            assertThat(agent.capabilities().supportsTts()).isTrue();
        }

        @Test
        @DisplayName("should create Vision agent")
        void shouldCreateVisionAgent() {
            Agent agent = Agent.visionAgent("vision-1", "VisionAgent");

            assertThat(agent.type()).isEqualTo(AgentType.VISION);
            assertThat(agent.capabilities().supportsVision()).isTrue();
        }

        @Test
        @DisplayName("should create Media agent")
        void shouldCreateMediaAgent() {
            Agent agent = Agent.mediaAgent("media-1", "MediaAgent");

            assertThat(agent.type()).isEqualTo(AgentType.MEDIA);
            assertThat(agent.capabilities().supportsMedia()).isTrue();
        }

        @Test
        @DisplayName("should create Text agent")
        void shouldCreateTextAgent() {
            Agent agent = Agent.textAgent("text-1", "TextAgent");

            assertThat(agent.type()).isEqualTo(AgentType.TEXT);
            assertThat(agent.capabilities().supportsText()).isTrue();
        }

        @Test
        @DisplayName("should create Supervisor agent with all capabilities")
        void shouldCreateSupervisorAgentWithAllCapabilities() {
            Agent agent = Agent.supervisorAgent("supervisor-1", "SupervisorAgent");

            assertThat(agent.type()).isEqualTo(AgentType.SUPERVISOR);
            assertThat(agent.capabilities().supportsRag()).isTrue();
            assertThat(agent.capabilities().supportsTts()).isTrue();
            assertThat(agent.capabilities().supportsVision()).isTrue();
            assertThat(agent.capabilities().supportsMedia()).isTrue();
            assertThat(agent.capabilities().supportsText()).isTrue();
            assertThat(agent.capabilities().supportsChat()).isTrue();
        }
    }

    @Nested
    @DisplayName("canHandle method")
    class CanHandleMethodTests {

        @Test
        @DisplayName("should return true when capabilities support the type")
        void shouldReturnTrueWhenCapabilitiesSupportTheType() {
            Agent agent = Agent.ragAgent("rag-1", "RagAgent");

            assertThat(agent.canHandle(AgentType.RAG)).isTrue();
        }

        @Test
        @DisplayName("should return false when capabilities do not support the type")
        void shouldReturnFalseWhenCapabilitiesDoNotSupportTheType() {
            Agent agent = Agent.ragAgent("rag-1", "RagAgent");

            assertThat(agent.canHandle(AgentType.TTS)).isFalse();
        }
    }

    @Nested
    @DisplayName("supports method")
    class SupportsMethodTests {

        @Test
        @DisplayName("should return true for same type")
        void shouldReturnTrueForSameType() {
            Agent agent = Agent.ragAgent("rag-1", "RagAgent");

            assertThat(agent.supports(AgentType.RAG)).isTrue();
        }

        @Test
        @DisplayName("should delegate to capabilities for other types")
        void shouldDelegateToCapabilitiesForOtherTypes() {
            Agent agent = Agent.ragAgent("rag-1", "RagAgent");

            assertThat(agent.supports(AgentType.RAG)).isTrue();
            assertThat(agent.supports(AgentType.CHAT)).isFalse();
        }
    }

    @Nested
    @DisplayName("canExecute method")
    class CanExecuteMethodTests {

        @Test
        @DisplayName("should return false when conversation is null")
        void shouldReturnFalseWhenConversationIsNull() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");

            assertThat(agent.canExecute(null)).isFalse();
        }

        @Test
        @DisplayName("should delegate to canContinue")
        void shouldDelegateToCanContinue() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");
            Conversation conversation = Conversation.start(
                    agent.id(),
                    Message.fromUser("Hello")
            );

            assertThat(agent.canExecute(conversation)).isTrue();
        }
    }

    @Nested
    @DisplayName("canContinue method")
    class CanContinueMethodTests {

        @Test
        @DisplayName("should return true when conversation can continue")
        void shouldReturnTrueWhenConversationCanContinue() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");
            Conversation conversation = Conversation.start(
                    agent.id(),
                    Message.fromUser("Hello")
            );

            assertThat(agent.canContinue(conversation)).isTrue();
        }
    }

    @Nested
    @DisplayName("getter methods")
    class GetterMethodTests {

        @Test
        @DisplayName("should return correct id value")
        void shouldReturnCorrectIdValue() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");

            assertThat(agent.getIdValue()).isEqualTo("chat-1");
        }

        @Test
        @DisplayName("should return correct name value")
        void shouldReturnCorrectNameValue() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");

            assertThat(agent.getNameValue()).isEqualTo("ChatAgent");
        }

        @Test
        @DisplayName("should return correct type id")
        void shouldReturnCorrectTypeId() {
            Agent agent = Agent.ragAgent("rag-1", "RagAgent");

            assertThat(agent.getTypeId()).isEqualTo("rag");
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal for same id")
        void shouldBeEqualForSameId() {
            Agent agent1 = Agent.chatAgent("chat-1", "ChatAgent1");
            Agent agent2 = Agent.chatAgent("chat-1", "ChatAgent2");

            assertThat(agent1).isEqualTo(agent2);
            assertThat(agent1.hashCode()).isEqualTo(agent2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different ids")
        void shouldNotBeEqualForDifferentIds() {
            Agent agent1 = Agent.chatAgent("chat-1", "ChatAgent");
            Agent agent2 = Agent.chatAgent("chat-2", "ChatAgent");

            assertThat(agent1).isNotEqualTo(agent2);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");

            assertThat(agent).isNotEqualTo("not an agent");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should return meaningful string representation")
        void shouldReturnMeaningfulStringRepresentation() {
            Agent agent = Agent.chatAgent("chat-1", "ChatAgent");

            String result = agent.toString();

            assertThat(result).contains("chat-1");
            assertThat(result).contains("ChatAgent");
            assertThat(result).contains("CHAT");
        }
    }
}
