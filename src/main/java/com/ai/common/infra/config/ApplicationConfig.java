package com.ai.common.infra.config;

import com.ai.chat.domain.service.LanguageDetectionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
  /** Creates the language detection service. */
  @Bean
  public LanguageDetectionService languageDetectionService() {
    return new LanguageDetectionService();
  }
}
