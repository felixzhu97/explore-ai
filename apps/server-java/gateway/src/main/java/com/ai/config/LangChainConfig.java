package com.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j configuration for AI model integration.
 */
@Configuration
public class LangChainConfig {

    @Value("${langchain4j.openai.api-key:EMPTY}")
    private String apiKey;

    @Value("${langchain4j.openai.model-name:deepseek-chat}")
    private String modelName;

    @Value("${langchain4j.openai.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
    }
}
