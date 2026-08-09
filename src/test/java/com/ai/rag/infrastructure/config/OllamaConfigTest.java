package com.ai.rag.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OllamaConfig")
class OllamaConfigTest {

    @Test
    @DisplayName("should create embedding and vision chat models")
    void shouldCreateEmbeddingAndVisionChatModels() {
        OllamaConfig config = new OllamaConfig();
        ReflectionTestUtils.setField(config, "baseUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(config, "embeddingModelName", "qwen3-embedding:0.6b");
        ReflectionTestUtils.setField(config, "visionModelName", "qwen3.5:35b");

        EmbeddingModel embeddingModel = config.embeddingModel();
        OllamaChatModel visionModel = config.ollamaVisionChatModel();

        assertThat(embeddingModel).isNotNull();
        assertThat(visionModel).isNotNull();
    }
}
