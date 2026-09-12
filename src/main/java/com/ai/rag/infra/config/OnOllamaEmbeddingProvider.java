package com.ai.rag.infra.config;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** Both Ollama embedding flag and provider=ollama (default). */
public class OnOllamaEmbeddingProvider extends AllNestedConditions {

  /** Nested condition phase for bean registration. */
  public OnOllamaEmbeddingProvider() {
    super(ConfigurationPhase.REGISTER_BEAN);
  }

  @ConditionalOnProperty(
      name = "spring.ai.ollama.embedding.enabled",
      havingValue = "true",
      matchIfMissing = true)
  static class EmbeddingEnabled {}

  @ConditionalOnProperty(
      name = "app.rag.embedding.provider",
      havingValue = "ollama",
      matchIfMissing = true)
  static class ProviderOllama {}
}
