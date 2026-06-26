package com.ai.shared.config;

import com.ai.ai.domain.repository.ChatSessionRepository;
import com.ai.ai.domain.service.LanguageDetectionService;
import com.ai.ai.infrastructure.store.InMemoryChatSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public ChatSessionRepository chatSessionRepository() {
        return new InMemoryChatSessionRepository();
    }

    @Bean
    public LanguageDetectionService languageDetectionService() {
        return new LanguageDetectionService();
    }
}
