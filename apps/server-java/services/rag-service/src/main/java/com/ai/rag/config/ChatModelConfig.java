package com.ai.rag.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.chat.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.anthropic.chat.AnthropicChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for chat language models (LLM).
 * Supports OpenAI, Ollama, and Anthropic providers.
 */
@Configuration
public class ChatModelConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatModelConfig.class);

    @Value("${rag.llm.provider:openai}")
    private String provider;

    @Value("${rag.llm.model-name:gpt-4o}")
    private String modelName;

    @Value("${rag.llm.api-key:}")
    private String apiKey;

    @Value("${rag.llm.base-url:}")
    private String baseUrl;

    @Value("${rag.llm.temperature:0.7}")
    private double temperature;

    @Value("${rag.llm.max-tokens:2048}")
    private int maxTokens;

    @Value("${rag.llm.timeout-seconds:60}")
    private int timeoutSeconds;

    @Bean
    public ChatModel chatModel() {
        log.info("Configuring chat model: provider={}, model={}", provider, modelName);

        return switch (provider.toLowerCase()) {
            case "ollama" -> OllamaChatModel.builder()
                    .baseUrl(baseUrl.isBlank() ? "http://localhost:11434" : baseUrl)
                    .modelName(modelName)
                    .temperature(temperature)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();

            default -> OpenAiChatModel.builder()
                    .apiKey(apiKey.isBlank() ? "dummy-key" : apiKey)
                    .modelName(modelName)
                    .baseUrl(baseUrl.isBlank() ? null : baseUrl)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .build();
        };
    }
}
