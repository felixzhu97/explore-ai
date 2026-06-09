package com.ai.agents.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoutingDecision Tests")
class RoutingDecisionTest {

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("should create routing decision with default values")
        void shouldCreateRoutingDecisionWithDefaultValues() {
            RoutingDecision decision = RoutingDecision.of(AgentType.RAG);

            assertThat(decision.targetType()).isEqualTo(AgentType.RAG);
            assertThat(decision.confidence()).isEqualTo(1.0);
            assertThat(decision.reason()).isEqualTo("Direct routing");
        }
    }

    @Nested
    @DisplayName("to factory methods")
    class ToFactoryMethodsTests {

        @Test
        @DisplayName("should create decision with type and reason")
        void shouldCreateDecisionWithTypeAndReason() {
            RoutingDecision decision = RoutingDecision.to(AgentType.TTS, "User requested TTS");

            assertThat(decision.targetType()).isEqualTo(AgentType.TTS);
            assertThat(decision.reason()).isEqualTo("User requested TTS");
            assertThat(decision.confidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should create decision with type, confidence and reason")
        void shouldCreateDecisionWithTypeConfidenceAndReason() {
            RoutingDecision decision = RoutingDecision.to(AgentType.VISION, 0.95, "High confidence match");

            assertThat(decision.targetType()).isEqualTo(AgentType.VISION);
            assertThat(decision.confidence()).isEqualTo(0.95);
            assertThat(decision.reason()).isEqualTo("High confidence match");
        }

        @Test
        @DisplayName("should create decision with agent id")
        void shouldCreateDecisionWithAgentId() {
            AgentId agentId = AgentId.of("chat-1");
            RoutingDecision decision = RoutingDecision.to(agentId, 0.9, "Specific agent routing");

            assertThat(decision.targetAgentId()).isEqualTo(agentId);
            assertThat(decision.confidence()).isEqualTo(0.9);
            assertThat(decision.targetType()).isNull();
        }

        @ParameterizedTest
        @ValueSource(doubles = {-0.1, -1.0, 1.1, 2.0})
        @DisplayName("should throw exception for invalid confidence values")
        void shouldThrowExceptionForInvalidConfidenceValues(double confidence) {
            assertThatThrownBy(() -> RoutingDecision.to(AgentType.CHAT, confidence, "reason"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Confidence must be between 0.0 and 1.0");
        }
    }

    @Nested
    @DisplayName("fallback factory method")
    class FallbackFactoryMethodTests {

        @Test
        @DisplayName("should create fallback decision to CHAT")
        void shouldCreateFallbackDecisionToChat() {
            RoutingDecision decision = RoutingDecision.fallback();

            assertThat(decision.targetType()).isEqualTo(AgentType.CHAT);
            assertThat(decision.confidence()).isEqualTo(0.5);
            assertThat(decision.reason()).isEqualTo("Fallback to chat");
        }
    }

    @Nested
    @DisplayName("isConfident method")
    class IsConfidentMethodTests {

        @Test
        @DisplayName("should return true for confidence >= 0.8")
        void shouldReturnTrueForConfidenceGreaterThanOrEqualTo08() {
            RoutingDecision decision = RoutingDecision.to(AgentType.RAG, 0.8, "reason");

            assertThat(decision.isConfident()).isTrue();
        }

        @Test
        @DisplayName("should return true for confidence > 0.8")
        void shouldReturnTrueForConfidenceGreaterThan08() {
            RoutingDecision decision = RoutingDecision.to(AgentType.RAG, 0.95, "reason");

            assertThat(decision.isConfident()).isTrue();
        }

        @Test
        @DisplayName("should return false for confidence < 0.8")
        void shouldReturnFalseForConfidenceLessThan08() {
            RoutingDecision decision = RoutingDecision.to(AgentType.RAG, 0.5, "reason");

            assertThat(decision.isConfident()).isFalse();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal for same values")
        void shouldBeEqualForSameValues() {
            RoutingDecision decision1 = RoutingDecision.to(AgentType.RAG, 0.9, "reason");
            RoutingDecision decision2 = RoutingDecision.to(AgentType.RAG, 0.9, "reason");

            assertThat(decision1).isEqualTo(decision2);
            assertThat(decision1.hashCode()).isEqualTo(decision2.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different types")
        void shouldNotBeEqualForDifferentTypes() {
            RoutingDecision decision1 = RoutingDecision.to(AgentType.RAG, 0.9, "reason");
            RoutingDecision decision2 = RoutingDecision.to(AgentType.TTS, 0.9, "reason");

            assertThat(decision1).isNotEqualTo(decision2);
        }

        @Test
        @DisplayName("should not be equal for different confidence")
        void shouldNotBeEqualForDifferentConfidence() {
            RoutingDecision decision1 = RoutingDecision.to(AgentType.RAG, 0.9, "reason");
            RoutingDecision decision2 = RoutingDecision.to(AgentType.RAG, 0.8, "reason");

            assertThat(decision1).isNotEqualTo(decision2);
        }
    }

    @Nested
    @DisplayName("null handling")
    class NullHandlingTests {

        @Test
        @DisplayName("fallback should create valid routing decision")
        void fallbackShouldCreateValidRoutingDecision() {
            RoutingDecision decision = RoutingDecision.fallback();

            assertThat(decision.targetType()).isEqualTo(AgentType.CHAT);
            assertThat(decision.confidence()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("should handle null reason by defaulting to empty string")
        void shouldHandleNullReasonByDefaultingToEmptyString() {
            RoutingDecision decision = RoutingDecision.to(AgentType.RAG, "explicit reason");

            assertThat(decision.reason()).isNotNull();
        }
    }
}
