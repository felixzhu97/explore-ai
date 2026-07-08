package com.ai.common.config;

import com.ai.chat.domain.service.LanguageDetectionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public LanguageDetectionService languageDetectionService() {
        return new LanguageDetectionService();
    }
}
