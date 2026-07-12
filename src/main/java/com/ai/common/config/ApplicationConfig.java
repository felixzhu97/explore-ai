package com.ai.common.config;

import com.ai.chat.domain.service.LanguageDetectionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppModuleProperties.class)
public class ApplicationConfig {

    @Bean
    public LanguageDetectionService languageDetectionService() {
        return new LanguageDetectionService();
    }
}
