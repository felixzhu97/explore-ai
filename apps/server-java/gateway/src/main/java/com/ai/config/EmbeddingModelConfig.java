package com.ai.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for embedding models via LangChain4j.
 */
@Configuration
public class EmbeddingModelConfig {

    @Value("${langchain4j.embedding.api-key:}")
    private String apiKey;

    @Value("${langchain4j.embedding.model-name:text-embedding-3-small}")
    private String modelName;

    @Value("${langchain4j.embedding.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .build();
    }
}
