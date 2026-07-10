package com.ai.rag.infrastructure.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Configuration for Ollama embedding and vision chat services.
 */
@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.embedding.model:nomic-embed-text}")
    private String embeddingModelName;

    @Value("${spring.ai.ollama.chat.model:qwen3.5:35b}")
    private String visionModelName;

    @Bean
    @ConditionalOnProperty(name = "spring.ai.ollama.embedding.enabled", havingValue = "true", matchIfMissing = true)
    @NonNull
    public EmbeddingModel embeddingModel() {
        OllamaApi api = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .options(options)
                .build();
    }

    @Bean("ollamaVisionChatModel")
    @ConditionalOnProperty(name = "spring.ai.ollama.chat.enabled", havingValue = "true", matchIfMissing = true)
    @NonNull
    public ChatModel ollamaVisionChatModel() {
        OllamaApi api = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        OllamaChatOptions options = OllamaChatOptions.builder()
                .model(visionModelName)
                .build();

        return OllamaChatModel.builder()
                .ollamaApi(api)
                .options(options)
                .build();
    }
}
