package com.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.openai.OpenAiChatModel;

@Configuration
public class SpringAIConfig {
    
    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
    }
}
