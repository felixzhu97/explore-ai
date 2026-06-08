package com.ai.rag.config;

import dev.langchain4j.embedding.EmbeddingModel;
import dev.langchain4j.embedding.onnx.HuggingFaceEmbeddingModel;
import dev.langchain4j.embedding.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for embedding models.
 * Supports HuggingFace and OpenAI-compatible embedding models.
 */
@Configuration
public class LangChain4jConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChain4jConfig.class);

    @Value("${rag.embedding.model-name:sentence-transformers/all-MiniLM-L6-v2}")
    private String embeddingModelName;

    @Value("${rag.embedding.provider:local}")
    private String embeddingProvider;

    @Value("${rag.embedding.base-url:}")
    private String embeddingBaseUrl;

    @Value("${rag.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${rag.embedding.dimension:384}")
    private int embeddingDimension;

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Configuring embedding model: provider={}, model={}", embeddingProvider, embeddingModelName);

        if ("openai".equalsIgnoreCase(embeddingProvider)) {
            return OpenAiEmbeddingModel.builder()
                    .apiKey(embeddingApiKey.isBlank() ? "dummy" : embeddingApiKey)
                    .modelName(embeddingModelName)
                    .baseUrl(embeddingBaseUrl.isBlank() ? null : embeddingBaseUrl)
                    .build();
        }

        // Default to HuggingFace local model
        return HuggingFaceEmbeddingModel.builder()
                .modelName(embeddingModelName)
                .build();
    }

    @Bean
    public int embeddingDimension(@Value("${rag.embedding.dimension:384}") int dimension) {
        return dimension;
    }
}
