package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgentId Tests")
class AgentIdTest {

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("should create AgentId with valid value")
        void shouldCreateAgentIdWithValidValue() {
            AgentId agentId = AgentId.of("chat-001");

            assertThat(agentId.value()).isEqualTo("chat-001");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should throw exception for null or blank value")
        void shouldThrowExceptionForNullOrBlankValue(String value) {
            assertThatThrownBy(() -> AgentId.of(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("should store value as is")
        void shouldStoreValueAsIs() {
            AgentId agentId = AgentId.of("  chat-001  ");

            assertThat(agentId.value()).isEqualTo("  chat-001  ");
        }
    }

    @Nested
    @DisplayName("value accessor")
    class ValueAccessorTests {

        @Test
        @DisplayName("should return the stored value")
        void shouldReturnTheStoredValue() {
            String value = "agent-123";
            AgentId agentId = AgentId.of(value);

            assertThat(agentId.value()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal for same value")
        void shouldBeEqualForSameValue() {
            AgentId id1 = AgentId.of("agent-001");
            AgentId id2 = AgentId.of("agent-001");

            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different values")
        void shouldNotBeEqualForDifferentValues() {
            AgentId id1 = AgentId.of("agent-001");
            AgentId id2 = AgentId.of("agent-002");

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            AgentId agentId = AgentId.of("agent-001");

            assertThat(agentId).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            AgentId agentId = AgentId.of("agent-001");

            assertThat(agentId).isNotEqualTo("agent-001");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should return value as string representation")
        void shouldReturnValueAsStringRepresentation() {
            AgentId agentId = AgentId.of("chat-agent");

            assertThat(agentId.toString()).isEqualTo("chat-agent");
        }
    }
}
