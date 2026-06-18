package com.ai.config;

import com.ai.domain.repository.ChatSessionRepository;
import com.ai.domain.service.LanguageDetectionService;
import com.ai.adapter.out.persistence.InMemoryChatSessionRepository;
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
