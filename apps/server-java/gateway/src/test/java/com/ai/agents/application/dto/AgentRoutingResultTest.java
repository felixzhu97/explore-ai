package com.ai.agents.application.dto;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.RoutingDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentRoutingResult Tests")
class AgentRoutingResultTest {

    @Nested
    @DisplayName("from factory method")
    class FromFactoryMethodTests {

        @Test
        @DisplayName("should create from RoutingDecision")
        void shouldCreateFromRoutingDecision() {
            RoutingDecision decision = RoutingDecision.to(AgentType.RAG, 0.9, "Matched RAG keywords");

            AgentRoutingResult result = AgentRoutingResult.from(decision);

            assertThat(result.targetType()).isEqualTo(AgentType.RAG);
            assertThat(result.confidence()).isEqualTo(0.9);
            assertThat(result.reason()).isEqualTo("Matched RAG keywords");
        }

        @Test
        @DisplayName("should preserve all decision properties")
        void shouldPreserveAllDecisionProperties() {
            RoutingDecision decision = RoutingDecision.to(AgentType.TTS, 0.95, "TTS request detected");

            AgentRoutingResult result = AgentRoutingResult.from(decision);

            assertThat(result.targetType()).isEqualTo(decision.targetType());
            assertThat(result.confidence()).isEqualTo(decision.confidence());
            assertThat(result.reason()).isEqualTo(decision.reason());
        }
    }

    @Nested
    @DisplayName("fallback factory method")
    class FallbackFactoryMethodTests {

        @Test
        @DisplayName("should create fallback result to CHAT")
        void shouldCreateFallbackResultToChat() {
            AgentRoutingResult result = AgentRoutingResult.fallback();

            assertThat(result.targetType()).isEqualTo(AgentType.CHAT);
            assertThat(result.confidence()).isEqualTo(0.5);
            assertThat(result.reason()).isEqualTo("Fallback to chat");
        }
    }

    @Nested
    @DisplayName("record accessors")
    class RecordAccessorTests {

        @Test
        @DisplayName("should provide access to all properties")
        void shouldProvideAccessToAllProperties() {
            AgentRoutingResult result = new AgentRoutingResult(
                    AgentType.VISION,
                    0.85,
                    "Vision keyword detected"
            );

            assertThat(result.targetType()).isEqualTo(AgentType.VISION);
            assertThat(result.confidence()).isEqualTo(0.85);
            assertThat(result.reason()).isEqualTo("Vision keyword detected");
        }
    }
}
