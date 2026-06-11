package com.ai.infrastructure.adapter.ai;

import com.ai.domain.model.ChatMessage;
import com.ai.domain.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring AI implementation of domain service interface.
 * Bridges Spring AI framework with domain layer.
 */
@Service
public class SpringAiChatService implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatService.class);

    private final ChatModel chatModel;

    public SpringAiChatService(@Qualifier("openAiChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String chatWithHistory(List<ChatMessage> messages) {
        log.info("Chat request with {} messages", messages.size());

        List<org.springframework.ai.chat.messages.Message> springMessages = messages.stream()
            .map(msg -> {
                if (msg.isFromUser()) {
                    return new UserMessage(msg.getText());
                } else {
                    return new AssistantMessage(msg.getText());
                }
            })
            .collect(java.util.stream.Collectors.toList());

        Prompt prompt = new Prompt(springMessages);

        try {
            ChatResponse response = chatModel.call(prompt);
            String text = response.getResult().getOutput().getText();
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
        UserMessage userMsg = new UserMessage(userMessage);
        Prompt prompt = new Prompt(userMsg);

        try {
            ChatResponse response = chatModel.call(prompt);
            String text = response.getResult().getOutput().getText();
            log.info("Chat response: {}", truncateForLog(text));
            return text != null ? text : "";
        } catch (Exception e) {
            log.error("Chat error", e);
            throw new RuntimeException("AI service error: " + e.getMessage(), e);
        }
    }

    private String truncateForLog(String text) {
        if (text == null) return "null";
        if (text.length() <= 100) return text;
        return text.substring(0, 100) + "...";
    }
}
