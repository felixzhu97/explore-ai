package com.ai.agents.application.service;

import com.ai.agents.application.dto.AgentRoutingResult;
import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.domain.Message;
import com.ai.agents.domain.service.AgentRegistry;
import com.ai.agents.domain.service.SupervisorAgent;
import com.ai.agents.infrastructure.adapter.AgentAdapter;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Use case for processing agent requests.
 */
@Service
public class ProcessAgentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessAgentUseCase.class);

    private final RouteMessageUseCase routeMessageUseCase;
    private final AgentRegistry agentRegistry;
    private final SupervisorAgent supervisorAgent;
    private final Map<AgentType, AgentAdapter> agentAdapters;

    public ProcessAgentUseCase(
            RouteMessageUseCase routeMessageUseCase,
            AgentRegistry agentRegistry,
            SupervisorAgent supervisorAgent,
            List<AgentAdapter> adapters
    ) {
        this.routeMessageUseCase = routeMessageUseCase;
        this.agentRegistry = agentRegistry;
        this.supervisorAgent = supervisorAgent;
        this.agentAdapters = adapters.stream()
                .collect(java.util.stream.Collectors.toMap(
                        adapter -> adapter.getType(),
                        adapter -> adapter
                ));
    }

    /**
     * Process an agent request.
     */
    public Mono<AgentResponseDto> process(AgentRequestDto request) {
        log.info("Processing request: type={}, message={}",
                request.agentType(), truncate(request.getUserMessage(), 50));

        AgentType targetType = request.agentType();
        String message = request.getUserMessage();

        // Step 1: Route message to determine target agent
        AgentRoutingResult routingResult = routeMessageUseCase.route(message);
        AgentType routedType = routingResult.targetType();

        // Step 2: Get adapter for the target type
        AgentAdapter adapter = agentAdapters.get(routedType);

        if (adapter == null) {
            log.warn("No adapter found for type: {}", routedType);
            return Mono.just(AgentResponseDto.error("Agent not available: " + routedType));
        }

        // Step 3: Process through adapter
        try {
            Conversation conversation = createConversation(routedType, message);
            Mono<AgentResponseDto> response = adapter.execute(conversation, request);
            
            return response
                    .map(responseDto -> responseDto.withRoutedTo(routedType))
                    .doOnSuccess(r -> log.info("Request processed successfully: type={}", routedType))
                    .doOnError(e -> log.error("Error processing request: {}", e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing request", e);
            return Mono.just(AgentResponseDto.error("Processing failed: " + e.getMessage()));
        }
    }

    /**
     * Process with explicit agent type (bypass routing).
     */
    public Mono<AgentResponseDto> processDirect(AgentRequestDto request, AgentType targetType) {
        log.info("Direct processing: target={}, message={}",
                targetType, truncate(request.getUserMessage(), 50));

        AgentAdapter adapter = agentAdapters.get(targetType);

        if (adapter == null) {
            log.warn("No adapter found for type: {}", targetType);
            return Mono.just(AgentResponseDto.error("Agent not available: " + targetType));
        }

        Conversation conversation = createConversation(targetType, request.getUserMessage());
        return adapter.execute(conversation, request)
                .doOnSuccess(r -> log.info("Direct request processed successfully: type={}", targetType))
                .doOnError(e -> log.error("Error in direct processing: {}", e.getMessage()));
    }

    /**
     * Get available agent types.
     */
    public List<AgentType> getAvailableTypes() {
        return agentAdapters.keySet().stream().toList();
    }

    /**
     * Check if a type is available.
     */
    public boolean isTypeAvailable(AgentType type) {
        return agentAdapters.containsKey(type);
    }

    private Conversation createConversation(AgentType type, String message) {
        return Conversation.start(null, Message.fromUser(message));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
