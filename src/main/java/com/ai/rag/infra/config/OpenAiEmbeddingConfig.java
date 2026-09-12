package com.ai.rag.infra.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/** Cloud embedding via OpenAI API (Render has no local Ollama). */
@Configuration
@ConditionalOnProperty(name = "app.rag.embedding.provider", havingValue = "openai")
@ConditionalOnExpression(
    "T(org.springframework.util.StringUtils).hasText('${OPENAI_API_KEY:}')"
        + " or T(org.springframework.util.StringUtils)"
        + ".hasText('${IMAGE_API_KEY:}')"
        + " or T(org.springframework.util.StringUtils)"
        + ".hasText('${app.rag.embedding.openai-api-key:}')")
public class OpenAiEmbeddingConfig {

  @Value("${app.rag.embedding.openai-api-key:${OPENAI_API_KEY:}}")
  private String apiKey;

  @Value("${app.rag.embedding.openai-base-url:https://api.openai.com}")
  private String baseUrl;

  @Value("${app.rag.embedding.model:text-embedding-3-small}")
  private String model;

  /** Documentation. */
  @Bean
  @NonNull
  public EmbeddingModel embeddingModel() {
    String normalized = baseUrl.trim();
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    ClientOptions clientOptions =
        ClientOptions.builder()
            .apiKey(apiKey)
            .baseUrl(normalized)
            .httpClient(SpringAiOpenAiHttpClient.builder().build())
            .build();
    OpenAIClient openAiClient = new OpenAIClientImpl(clientOptions);
    OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder().model(model).build();
    return OpenAiEmbeddingModel.builder().openAiClient(openAiClient).options(options).build();
  }
}
