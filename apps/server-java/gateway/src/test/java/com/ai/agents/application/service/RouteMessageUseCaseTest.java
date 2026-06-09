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
    @DisplayName("route method")
    class RouteMethodTests {

        @Test
        @DisplayName("should route message and return routing result")
        void shouldRouteMessageAndReturnRoutingResult() {
            String message = "search for documents";
            RoutingDecision decision = RoutingDecision.to(AgentType.RAG, 0.9, "Matched RAG keywords");
            when(supervisorAgent.route(message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.route(message);

            assertThat(result.targetType()).isEqualTo(AgentType.RAG);
            assertThat(result.confidence()).isEqualTo(0.9);
            assertThat(result.reason()).isEqualTo("Matched RAG keywords");
        }

        @Test
        @DisplayName("should delegate routing to supervisor agent")
        void shouldDelegateRoutingToSupervisorAgent() {
            String message = "translate to English";
            when(supervisorAgent.route(message)).thenReturn(RoutingDecision.to(AgentType.TEXT, "Text intent"));

            routeMessageUseCase.route(message);

            verify(supervisorAgent).route(message);
        }
    }

    @Nested
    @DisplayName("routeWithPreference method")
    class RouteWithPreferenceMethodTests {

        @Test
        @DisplayName("should route with preference and return result")
        void shouldRouteWithPreferenceAndReturnResult() {
            String message = "speak this text";
            AgentType preferredType = AgentType.TTS;
            RoutingDecision decision = RoutingDecision.to(AgentType.TTS, 1.0, "Explicit TTS preference");
            when(supervisorAgent.routeTo(preferredType, message)).thenReturn(decision);

            AgentRoutingResult result = routeMessageUseCase.routeWithPreference(message, preferredType);

            assertThat(result.targetType()).isEqualTo(AgentType.TTS);
            assertThat(result.confidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should delegate routing with preference to supervisor agent")
        void shouldDelegateRoutingWithPreferenceToSupervisorAgent() {
            String message = "analyze image";
            AgentType preferredType = AgentType.VISION;
            when(supervisorAgent.routeTo(preferredType, message)).thenReturn(RoutingDecision.of(AgentType.VISION));

            routeMessageUseCase.routeWithPreference(message, preferredType);

            verify(supervisorAgent).routeTo(preferredType, message);
        }
    }

    @Nested
    @DisplayName("isAgentAvailable method")
    class IsAgentAvailableMethodTests {

        @Test
        @DisplayName("should return true when agent is registered")
        void shouldReturnTrueWhenAgentIsRegistered() {
            when(agentRegistry.hasAgent(AgentType.RAG)).thenReturn(true);

            boolean available = routeMessageUseCase.isAgentAvailable(AgentType.RAG);

            assertThat(available).isTrue();
        }

        @Test
        @DisplayName("should return false when agent is not registered")
        void shouldReturnFalseWhenAgentIsNotRegistered() {
            when(agentRegistry.hasAgent(AgentType.TTS)).thenReturn(false);

            boolean available = routeMessageUseCase.isAgentAvailable(AgentType.TTS);

            assertThat(available).isFalse();
        }

        @Test
        @DisplayName("should delegate check to agent registry")
        void shouldDelegateCheckToAgentRegistry() {
            AgentType type = AgentType.MEDIA;
            when(agentRegistry.hasAgent(type)).thenReturn(true);

            routeMessageUseCase.isAgentAvailable(type);

            verify(agentRegistry).hasAgent(type);
        }
    }
}
