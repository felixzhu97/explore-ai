package com.ai.infrastructure.config;

import com.ai.application.port.AiChatPort;
import com.ai.application.port.ChatSessionRepositoryPort;
import com.ai.application.service.ChatApplicationService;
import com.ai.domain.service.AiChatService;
import com.ai.infrastructure.adapter.ai.SpringAiChatAdapter;
import com.ai.infrastructure.adapter.ai.SpringAiChatService;
import com.ai.infrastructure.adapter.persistence.InMemoryChatSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application configuration class - manages dependency injection.
 * Connects infrastructure layer with domain/application layers.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public AiChatService aiChatService(SpringAiChatService springAiChatService) {
        return springAiChatService;
    }

    @Bean
    public AiChatPort aiChatPort(SpringAiChatAdapter springAiChatAdapter) {
        return springAiChatAdapter;
    }

    @Bean
    public InMemoryChatSessionRepository chatSessionRepository() {
        return new InMemoryChatSessionRepository();
    }

    @Bean
    public ChatApplicationService chatApplicationService(
            ChatSessionRepositoryPort repositoryPort,
            AiChatPort aiChatPort) {
        return new ChatApplicationService(repositoryPort, aiChatPort);
    }
}
