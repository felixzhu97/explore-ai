package com.ai.infrastructure.adapter.ai;

import com.ai.application.port.AiChatPort;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Spring AI adapter - implements AI chat port.
 * Adapts Spring AI framework to application layer interface.
 */
@Component
public class SpringAiChatAdapter implements AiChatPort {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatAdapter.class);

    private final AiChatService aiChatService;

    public SpringAiChatAdapter(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @Override
    public String chat(String userMessage) {
        log.info("Sending message to AI: {}", truncateForLog(userMessage));
        try {
            String response = aiChatService.chat(userMessage);
            log.info("Received AI response: {}", truncateForLog(response));
            return response;
        } catch (Exception e) {
            log.error("AI chat failed", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    public String chatWithHistory(List<ChatMessage> messages) {
        log.info("Sending {} messages to AI with history", messages.size());
        try {
            String response = aiChatService.chatWithHistory(messages);
            log.info("Received AI response with history: {}", truncateForLog(response));
            return response;
        } catch (Exception e) {
            log.error("AI chat with history failed", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        log.info("Sending streaming message to AI: {}", truncateForLog(userMessage));
        try {
            return aiChatService.chatStream(userMessage)
                .doOnNext(chunk -> log.debug("Stream chunk received: {} chars", chunk.length()))
                .doOnError(e -> log.error("AI stream failed", e));
        } catch (Exception e) {
            log.error("AI chat stream failed", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 100) return text;
        return text.substring(0, 100) + "...";
    }
}
