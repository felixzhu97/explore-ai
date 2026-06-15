package com.ai.infrastructure.adapter.ai;

import com.ai.domain.model.ChatMessage;
import com.ai.domain.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * Spring AI implementation of domain service interface.
 * Bridges Spring AI framework with domain layer using ChatClient builder pattern.
 */
@Service
public class SpringAiChatService implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatService.class);

    private final ChatClient chatClient;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public SpringAiChatService(ChatClient.Builder chatClientBuilder,
                               MessageChatMemoryAdvisor memoryAdvisor) {
        this.chatClient = chatClientBuilder.build();
        this.memoryAdvisor = memoryAdvisor;
    }

    @Override
    public String chatWithHistory(List<ChatMessage> messages) {
        log.info("Chat request with {} messages", messages.size());

        List<Message> springMessages = messages.stream()
            .map(msg -> {
                if (msg.isFromUser()) {
                    return (Message) new UserMessage(msg.getText());
                } else {
                    return (Message) new AssistantMessage(msg.getText());
                }
            })
            .collect(java.util.stream.Collectors.toList());

        try {
            String text = chatClient.prompt()
                .messages(springMessages)
                .call()
                .content();
            log.info("Chat response received: {} characters", text != null ? text.length() : 0);
            return text != null ? text : "";
        } catch (Exception e) {
            log.error("Chat error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(String userMessage) {
        log.info("Simple chat request: {}", truncateForLog(userMessage));

        try {
            String text = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
            log.info("Chat response: {}", truncateForLog(text));
            return text != null ? text : "";
        } catch (Exception e) {
            log.error("Chat error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(String userMessage) {
        log.info("Streaming chat request: {}", truncateForLog(userMessage));

        return chatClient.prompt()
            .user(userMessage)
            .stream()
            .content()
            .bufferTimeout(150, Duration.ofMillis(200))
            .map(list -> String.join("", list))
            .doOnError(e -> log.error("Stream error", e))
            .doOnComplete(() -> log.info("Stream completed"));
    }

    @Override
    public Flux<String> chatStream(String userMessage, String systemPrompt) {
        log.info("Streaming chat request with system prompt: {}", truncateForLog(userMessage));

        return chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .stream()
            .content()
            .bufferTimeout(150, Duration.ofMillis(200))
            .map(list -> String.join("", list))
            .doOnError(e -> log.error("Stream error", e))
            .doOnComplete(() -> log.info("Stream completed"));
    }

    /**
     * Sends a message to AI with memory support using conversation ID.
     *
     * @param userMessage user message text
     * @param conversationId unique conversation identifier
     * @return AI response text
     */
    public String chatWithMemory(String userMessage, String conversationId) {
        log.info("Chat with memory request: {}, conversationId: {}", truncateForLog(userMessage), conversationId);

        try {
            String text = chatClient.prompt()
                .user(userMessage)
                .advisors(advisorSpec -> advisorSpec.param("conversationId", conversationId))
                .call()
                .content();
            log.info("Chat with memory response: {} characters", text != null ? text.length() : 0);
            return text != null ? text : "";
        } catch (Exception e) {
            log.error("Chat with memory error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 100) return text;
        return text.substring(0, 100) + "...";
    }
}
