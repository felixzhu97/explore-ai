package com.ai.common.config;

import com.ai.chat.domain.repository.ChatSessionRepository;
import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.chat.infrastructure.store.InMemoryChatSessionRepository;
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
