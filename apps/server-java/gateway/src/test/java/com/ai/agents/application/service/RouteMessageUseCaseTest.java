package com.ai.agents.application.service;

import com.ai.agents.application.dto.AgentRoutingResult;
import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.RoutingDecision;
import com.ai.agents.domain.service.AgentRegistry;
import com.ai.agents.domain.service.SupervisorAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RouteMessageUseCase Tests")
class RouteMessageUseCaseTest {

    @Mock
    private SupervisorAgent supervisorAgent;

    @Mock
    private AgentRegistry agentRegistry;

    private RouteMessageUseCase routeMessageUseCase;

    @BeforeEach
    void setUp() {
        routeMessageUseCase = new RouteMessageUseCase(supervisorAgent, agentRegistry);
    }

    @Nested
    @DisplayName("route")
    class RouteTests {

        @Test
        @DisplayName("should return routing result from supervisor agent")
        void shouldReturnRoutingResultFromSupervisor() {
            String message = "search for documents";
            RoutingDecision decision = RoutingDecision.to(AgentType.RAG, 0.9, "Matched keyword from RAG");

            when(supervisorAgent.route(message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.route(message);

            assertThat(result.targetType()).isEqualTo(AgentType.RAG);
            assertThat(result.confidence()).isEqualTo(0.9);
            assertThat(result.reason()).isEqualTo("Matched keyword from RAG");
            verify(supervisorAgent).route(message);
        }

        @Test
        @DisplayName("should route to chat when no keywords match")
        void shouldRouteToChatWhenNoKeywordsMatch() {
            String message = "hello world";
            RoutingDecision decision = RoutingDecision.fallback();

            when(supervisorAgent.route(message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.route(message);

            assertThat(result.targetType()).isEqualTo(AgentType.CHAT);
            assertThat(result.confidence()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("should handle empty message")
        void shouldHandleEmptyMessage() {
            String message = "";
            RoutingDecision decision = RoutingDecision.fallback();

            when(supervisorAgent.route(message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.route(message);

            assertThat(result).isNotNull();
            verify(supervisorAgent).route(message);
        }

        @Test
        @DisplayName("should handle null message")
        void shouldHandleNullMessage() {
            RoutingDecision decision = RoutingDecision.fallback();

            when(supervisorAgent.route(null)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.route(null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("routeWithPreference")
    class RouteWithPreferenceTests {

        @Test
        @DisplayName("should route to preferred agent type")
        void shouldRouteToPreferredAgentType() {
            String message = "generate an image";
            RoutingDecision decision = RoutingDecision.to(AgentType.MEDIA, 1.0, "Explicit routing with keyword match");

            when(supervisorAgent.routeTo(AgentType.MEDIA, message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.routeWithPreference(message, AgentType.MEDIA);

            assertThat(result.targetType()).isEqualTo(AgentType.MEDIA);
            assertThat(result.confidence()).isEqualTo(1.0);
            verify(supervisorAgent).routeTo(AgentType.MEDIA, message);
        }

        @Test
        @DisplayName("should use explicit routing with confidence 1.0")
        void shouldUseExplicitRoutingWithFullConfidence() {
            String message = "translate to English";
            RoutingDecision decision = RoutingDecision.to(AgentType.TEXT, 1.0, "Explicit routing with keyword match");

            when(supervisorAgent.routeTo(AgentType.TEXT, message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.routeWithPreference(message, AgentType.TEXT);

            assertThat(result.confidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should handle null preferred type")
        void shouldHandleNullPreferredType() {
            String message = "some message";
            RoutingDecision decision = RoutingDecision.of(AgentType.CHAT);

            when(supervisorAgent.routeTo(null, message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.routeWithPreference(message, null);

            assertThat(result.targetType()).isEqualTo(AgentType.CHAT);
        }
    }

    @Nested
    @DisplayName("isAgentAvailable")
    class IsAgentAvailableTests {

        @Test
        @DisplayName("should return true when agent is available")
        void shouldReturnTrueWhenAgentAvailable() {
            when(agentRegistry.hasAgent(AgentType.RAG)).thenReturn(true);

            boolean available = routeMessageUseCase.isAgentAvailable(AgentType.RAG);

            assertThat(available).isTrue();
            verify(agentRegistry).hasAgent(AgentType.RAG);
        }

        @Test
        @DisplayName("should return false when agent is not available")
        void shouldReturnFalseWhenAgentNotAvailable() {
            when(agentRegistry.hasAgent(AgentType.VISION)).thenReturn(false);

            boolean available = routeMessageUseCase.isAgentAvailable(AgentType.VISION);

            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("should check multiple agent types")
        void shouldCheckMultipleAgentTypes() {
            when(agentRegistry.hasAgent(AgentType.TTS)).thenReturn(true);
            when(agentRegistry.hasAgent(AgentType.MEDIA)).thenReturn(false);

            assertThat(routeMessageUseCase.isAgentAvailable(AgentType.TTS)).isTrue();
            assertThat(routeMessageUseCase.isAgentAvailable(AgentType.MEDIA)).isFalse();
        }
    }

    @Nested
    @DisplayName("message truncation")
    class MessageTruncationTests {

        @Test
        @DisplayName("should not truncate short messages")
        void shouldNotTruncateShortMessages() {
            String shortMessage = "hello";
            RoutingDecision decision = RoutingDecision.to(AgentType.CHAT, 0.9, "Short message");

            when(supervisorAgent.route(shortMessage)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.route(shortMessage);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should handle very long messages")
        void shouldHandleVeryLongMessages() {
            String longMessage = "a".repeat(500);
            RoutingDecision decision = RoutingDecision.fallback();

            when(supervisorAgent.route(longMessage)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.route(longMessage);

            assertThat(result).isNotNull();
        }
    }
}
