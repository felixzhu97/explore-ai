package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgentName Tests")
class AgentNameTest {

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("should create AgentName with valid value")
        void shouldCreateAgentNameWithValidValue() {
            AgentName agentName = AgentName.of("ChatAgent");

            assertThat(agentName.value()).isEqualTo("ChatAgent");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should throw exception for null or blank value")
        void shouldThrowExceptionForNullOrBlankValue(String value) {
            assertThatThrownBy(() -> AgentName.of(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception for name exceeding 100 characters")
        void shouldThrowExceptionForNameExceeding100Characters() {
            String longName = "a".repeat(101);

            assertThatThrownBy(() -> AgentName.of(longName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot exceed 100 characters");
        }

        @Test
        @DisplayName("should accept name with exactly 100 characters")
        void shouldAcceptNameWithExactly100Characters() {
            String name = "a".repeat(100);

            AgentName agentName = AgentName.of(name);

            assertThat(agentName.value()).hasSize(100);
        }

        @Test
        @DisplayName("should trim whitespace from value")
        void shouldTrimWhitespaceFromValue() {
            AgentName agentName = AgentName.of("  ChatAgent  ");

            assertThat(agentName.value()).isEqualTo("ChatAgent");
        }
    }

    @Nested
    @DisplayName("value accessor")
    class ValueAccessorTests {

        @Test
        @DisplayName("should return the stored value")
        void shouldReturnTheStoredValue() {
            String value = "TestAgent";
            AgentName agentName = AgentName.of(value);

            assertThat(agentName.value()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal for same value")
        void shouldBeEqualForSameValue() {
            AgentName name1 = AgentName.of("ChatAgent");
            AgentName name2 = AgentName.of("ChatAgent");

            assertThat(name1).isEqualTo(name2);
            assertThat(name1.hashCode()).isEqualTo(name2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different values")
        void shouldNotBeEqualForDifferentValues() {
            AgentName name1 = AgentName.of("Agent1");
            AgentName name2 = AgentName.of("Agent2");

            assertThat(name1).isNotEqualTo(name2);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            AgentName agentName = AgentName.of("TestAgent");

            assertThat(agentName).isNotEqualTo("TestAgent");
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should return value as string representation")
        void shouldReturnValueAsStringRepresentation() {
            AgentName agentName = AgentName.of("MyAgent");

            assertThat(agentName.toString()).isEqualTo("MyAgent");
        }
    }
}
