package com.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j configuration for AI model integration.
 * Supports DeepSeek V4 Flash for chat with both streaming and non-streaming models.
 */
@Configuration
public class LangChainConfig {

    @Value("${langchain4j.chat.api-key:}")
    private String chatApiKey;

    @Value("${langchain4j.chat.model-name:deepseek-v4-flash}")
    private String chatModelName;

    @Value("${langchain4j.chat.base-url:https://api.deepseek.com/v1}")
    private String chatBaseUrl;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .baseUrl(chatBaseUrl)
                .temperature(0.7)
                .maxTokens(2000)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .baseUrl(chatBaseUrl)
                .temperature(0.7)
                .maxTokens(2000)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
