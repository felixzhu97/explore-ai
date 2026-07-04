package com.ai.rag.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Configuration for Ollama embedding service.
 * Conditionally creates an Ollama embedding model for local inference.
 */
@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.embedding.model:nomic-embed-text}")
    private String modelName;

    @Bean
    @ConditionalOnProperty(name = "spring.ai.ollama.embedding.enabled", havingValue = "true", matchIfMissing = true)
    @NonNull
    public EmbeddingModel embeddingModel() {
        OllamaApi api = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();

        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model(modelName)
                .build();

        return OllamaEmbeddingModel.builder()
                .ollamaApi(api)
                .options(options)
                .build();
    }
}
