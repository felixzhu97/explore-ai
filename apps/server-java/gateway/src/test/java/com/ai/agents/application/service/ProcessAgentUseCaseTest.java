package com.ai.agents.application.service;

import com.ai.agents.application.dto.AgentRoutingResult;
import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.domain.service.AgentRegistry;
import com.ai.agents.domain.service.SupervisorAgent;
import com.ai.agents.infrastructure.adapter.AgentAdapter;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessAgentUseCase Tests")
class ProcessAgentUseCaseTest {

    @Mock
    private RouteMessageUseCase routeMessageUseCase;

    @Mock
    private AgentRegistry agentRegistry;

    @Mock
    private SupervisorAgent supervisorAgent;

    @Mock
    private AgentAdapter ragAdapter;

    @Mock
    private AgentAdapter chatAdapter;

    private ProcessAgentUseCase processAgentUseCase;

    @BeforeEach
    void setUp() {
        when(ragAdapter.getType()).thenReturn(AgentType.RAG);
        when(chatAdapter.getType()).thenReturn(AgentType.CHAT);

        List<AgentAdapter> adapters = List.of(ragAdapter, chatAdapter);
        processAgentUseCase = new ProcessAgentUseCase(
                routeMessageUseCase, agentRegistry, supervisorAgent, adapters
        );
    }

    @Nested
    @DisplayName("process")
    class ProcessTests {

        @Test
        @DisplayName("should process request through routed agent")
        void shouldProcessRequestThroughRoutedAgent() {
            AgentRequestDto request = new AgentRequestDto(
                    "What is RAG?", AgentType.CHAT, null, null, null, null
            );
            AgentRoutingResult routingResult = new AgentRoutingResult(
                    AgentType.RAG, 0.9, "Intent detected"
            );
            AgentResponseDto adapterResponse = AgentResponseDto.success(
                    "RAG response", AgentType.RAG
            );

            when(routeMessageUseCase.route(request.message()))
                    .thenReturn(routingResult);
            when(ragAdapter.execute(any(Conversation.class), eq(request)))
                    .thenReturn(Mono.just(adapterResponse));

            StepVerifier.create(processAgentUseCase.process(request))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo("RAG response");
                        assertThat(response.agentType()).isEqualTo(AgentType.RAG);
                        assertThat(response.metadata()).containsKey("routedTo");
                    })
                    .verifyComplete();

            verify(routeMessageUseCase).route(request.message());
            verify(ragAdapter).execute(any(Conversation.class), eq(request));
        }

        @Test
        @DisplayName("should return error when adapter not found")
        void shouldReturnErrorWhenAdapterNotFound() {
            AgentRequestDto request = new AgentRequestDto(
                    "Test", AgentType.TTS, null, null, null, null
            );
            AgentRoutingResult routingResult = new AgentRoutingResult(
                    AgentType.TTS, 0.9, "TTS intent"
            );

            when(routeMessageUseCase.route(request.message()))
                    .thenReturn(routingResult);

            StepVerifier.create(processAgentUseCase.process(request))
                    .assertNext(response -> {
                        assertThat(response.error()).contains("not available");
                        assertThat(response.error()).contains("TTS");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("should propagate error from adapter execution")
        void shouldPropagateErrorFromAdapterExecution() {
            AgentRequestDto request = new AgentRequestDto(
                    "Test message", AgentType.CHAT, null, null, null, null
            );
            AgentRoutingResult routingResult = new AgentRoutingResult(
                    AgentType.CHAT, 0.9, "Chat intent"
            );

            when(routeMessageUseCase.route(request.message()))
                    .thenReturn(routingResult);
            when(chatAdapter.execute(any(Conversation.class), eq(request)))
                    .thenReturn(Mono.error(new RuntimeException("Execution failed")));

            StepVerifier.create(processAgentUseCase.process(request))
                    .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().contains("Execution failed"))
                    .verify();
        }

        @Test
        @DisplayName("should include routing metadata in response")
        void shouldIncludeRoutingMetadataInResponse() {
            AgentRequestDto request = new AgentRequestDto(
                    "RAG query", null, null, null, null, null
            );
            AgentRoutingResult routingResult = new AgentRoutingResult(
                    AgentType.RAG, 0.95, "High confidence"
            );
            AgentResponseDto adapterResponse = AgentResponseDto.success(
                    "Response", AgentType.RAG
            );

            when(routeMessageUseCase.route(request.message()))
                    .thenReturn(routingResult);
            when(ragAdapter.execute(any(Conversation.class), eq(request)))
                    .thenReturn(Mono.just(adapterResponse));

            StepVerifier.create(processAgentUseCase.process(request))
                    .assertNext(response -> {
                        assertThat(response.metadata()).isNotNull();
                        assertThat(response.metadata().get("routedTo")).isEqualTo("RAG");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("processDirect")
    class ProcessDirectTests {

        @Test
        @DisplayName("should process directly with specified agent type")
        void shouldProcessDirectlyWithSpecifiedAgentType() {
            AgentRequestDto request = new AgentRequestDto(
                    "Direct message", AgentType.CHAT, null, null, null, null
            );
            AgentType targetType = AgentType.RAG;
            AgentResponseDto adapterResponse = AgentResponseDto.success(
                    "Direct response", AgentType.RAG
            );

            when(ragAdapter.execute(any(Conversation.class), eq(request)))
                    .thenReturn(Mono.just(adapterResponse));

            StepVerifier.create(processAgentUseCase.processDirect(request, targetType))
                    .assertNext(response -> {
                        assertThat(response.message()).isEqualTo("Direct response");
                    })
                    .verifyComplete();

            verify(routeMessageUseCase, never()).route(any());
        }

        @Test
        @DisplayName("should return error when direct target adapter not found")
        void shouldReturnErrorWhenDirectTargetAdapterNotFound() {
            AgentRequestDto request = new AgentRequestDto(
                    "Test", AgentType.CHAT, null, null, null, null
            );
            AgentType targetType = AgentType.MEDIA;

            StepVerifier.create(processAgentUseCase.processDirect(request, targetType))
                    .assertNext(response -> {
                        assertThat(response.error()).contains("not available");
                        assertThat(response.error()).contains("MEDIA");
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getAvailableTypes")
    class GetAvailableTypesTests {

        @Test
        @DisplayName("should return all registered agent types")
        void shouldReturnAllRegisteredAgentTypes() {
            List<AgentType> availableTypes = processAgentUseCase.getAvailableTypes();

            assertThat(availableTypes).containsExactlyInAnyOrder(AgentType.RAG, AgentType.CHAT);
        }

        @Test
        @DisplayName("should not return unregistered types")
        void shouldNotReturnUnregisteredTypes() {
            List<AgentType> availableTypes = processAgentUseCase.getAvailableTypes();

            assertThat(availableTypes).doesNotContain(AgentType.TTS);
            assertThat(availableTypes).doesNotContain(AgentType.VISION);
        }
    }

    @Nested
    @DisplayName("isTypeAvailable")
    class IsTypeAvailableTests {

        @Test
        @DisplayName("should return true for registered type")
        void shouldReturnTrueForRegisteredType() {
            assertThat(processAgentUseCase.isTypeAvailable(AgentType.RAG)).isTrue();
            assertThat(processAgentUseCase.isTypeAvailable(AgentType.CHAT)).isTrue();
        }

        @Test
        @DisplayName("should return false for unregistered type")
        void shouldReturnFalseForUnregisteredType() {
            assertThat(processAgentUseCase.isTypeAvailable(AgentType.TTS)).isFalse();
            assertThat(processAgentUseCase.isTypeAvailable(AgentType.VISION)).isFalse();
            assertThat(processAgentUseCase.isTypeAvailable(AgentType.MEDIA)).isFalse();
        }
    }
}
