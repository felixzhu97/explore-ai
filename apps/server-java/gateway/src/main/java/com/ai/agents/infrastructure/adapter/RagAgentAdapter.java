package com.ai.agents.infrastructure.adapter;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import com.ai.rag.application.service.RagChatApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * RAG Agent Adapter.
 * Delegates to internal RagChatApplicationService instead of external WebClient call.
 */
@Component
public class RagAgentAdapter implements AgentAdapter {

    private static final Logger log = LoggerFactory.getLogger(RagAgentAdapter.class);

    private final RagChatApplicationService ragChatService;

    public RagAgentAdapter(RagChatApplicationService ragChatService) {
        this.ragChatService = ragChatService;
    }

    @Override
    public AgentType getType() {
        return AgentType.RAG;
    }

    @Override
    public Mono<AgentResponseDto> execute(Conversation conversation, AgentRequestDto request) {
        log.info("RAG agent processing request: {}", truncate(request.getUserMessage(), 50));

        String query = request.getUserMessage();
        int topK = request.topK() != null ? request.topK() : 5;

        return Mono.fromCallable(() -> {
            try {
                var response = ragChatService.chat(
                        new com.ai.rag.application.dto.ChatRequest(query, request.sessionId(), topK, null, null)
                );

                if (response.sources() != null && !response.sources().isEmpty()) {
                    var sourceStrings = response.sources().stream()
                            .map(src -> src.text())
                            .toList();
                    return AgentResponseDto.successWithSources(response.answer(), AgentType.RAG, sourceStrings);
                }

                return AgentResponseDto.success(response.answer(), AgentType.RAG);
            } catch (Exception e) {
                log.error("RAG processing failed", e);
                return AgentResponseDto.error("RAG failed: " + e.getMessage());
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    @Override
    public boolean isAvailable() {
        return ragChatService != null;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
