package com.ai.chat.application.usecase;

import com.ai.chat.domain.model.ChatSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Generates concise session titles from the first user-assistant exchange.
 */
@Service
public class SessionTitleGenerator {

    private static final Logger log = LoggerFactory.getLogger(SessionTitleGenerator.class);
    private static final int MAX_TITLE_LENGTH = 50;

    private static final String SYSTEM_PROMPT = """
            Generate a short chat title (max 50 characters, no quotes or punctuation at edges).
            Use the same language as the user's message. Return only the title text.
            """;

    private final ChatClient chatClient;

    public SessionTitleGenerator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String generate(String userMessage, String assistantReply) {
        try {
            String title = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("User: %s%nAssistant: %s".formatted(userMessage, assistantReply))
                    .call()
                    .content();
            if (title != null && !title.isBlank()) {
                return sanitize(title);
            }
        } catch (Exception e) {
            log.warn("Failed to generate session title via LLM, using fallback", e);
        }
        return fallback(userMessage);
    }

    String fallback(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return ChatSession.DEFAULT_TITLE;
        }
        String cleaned = userMessage.replaceAll("\\s+", " ").trim();
        if (cleaned.length() <= MAX_TITLE_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_TITLE_LENGTH);
    }

    private String sanitize(String title) {
        String cleaned = title.strip().replaceAll("^[\"']+|[\"']+$", "");
        if (cleaned.length() <= MAX_TITLE_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_TITLE_LENGTH);
    }
}
