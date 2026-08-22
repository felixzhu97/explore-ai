package com.ai.chat.service.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TextProviderCatalog")
class TextProviderCatalogTest {

  @Test
  void shouldListOpenaiAsAvailableProvider() {
    var catalog = new TextProviderCatalog(false, "");

    var providers = catalog.listProviders();

    assertThat(providers).hasSize(3);
    assertThat(providers.getFirst().name()).isEqualTo("openai");
    assertThat(providers.getFirst().status()).isEqualTo("available");
    assertThat(providers.getFirst().displayName()).isEqualTo("DeepSeek");
  }

  @Test
  void shouldReturnDeepseekModelsForOpenaiProvider() {
    var catalog = new TextProviderCatalog(false, "");

    var models = catalog.listModels("openai");

    assertThat(models)
        .extracting(m -> m.name())
        .containsExactly("deepseek-v4-flash", "deepseek-v4-pro");
    assertThat(models.getFirst().provider()).isEqualTo("openai");
    assertThat(models.getFirst().description()).isEqualTo("DeepSeek V4 Flash");
  }

  @Test
  void shouldReturnAnthropicCurrentAndLegacyModels() {
    var catalog = new TextProviderCatalog(false, "sk-ant-test");

    var models = catalog.listModels("anthropic");

    assertThat(models)
        .extracting(m -> m.name())
        .contains(
            "claude-fable-5",
            "claude-opus-4-8",
            "claude-sonnet-5",
            "claude-haiku-4-5-20251001",
            "claude-opus-4-7",
            "claude-sonnet-4-6");
  }

  @Test
  void shouldReturnOllamaChatModelTags() {
    var catalog = new TextProviderCatalog(true, "");

    var models = catalog.listModels("ollama");

    assertThat(models)
        .extracting(m -> m.name())
        .contains("qwen3.5:35b", "qwen3:8b", "llama3.2", "mistral");
  }

  @Test
  void shouldMarkOllamaUnavailableWhenChatDisabled() {
    var catalog = new TextProviderCatalog(false, "");

    var providers = catalog.listProviders();

    var ollama =
        providers.stream().filter(p -> "ollama".equals(p.name())).findFirst().orElseThrow();
    assertThat(ollama.status()).isEqualTo("unavailable");
  }

  @Test
  void shouldMarkAnthropicAvailableWhenApiKeyConfigured() {
    var catalog = new TextProviderCatalog(false, "sk-ant-test");

    var anthropic =
        catalog.listProviders().stream()
            .filter(p -> "anthropic".equals(p.name()))
            .findFirst()
            .orElseThrow();

    assertThat(anthropic.status()).isEqualTo("available");
  }

  @Test
  void shouldDefaultToOpenaiModelsWhenProviderMissing() {
    var catalog = new TextProviderCatalog(false, "");

    var models = catalog.listModels(null);

    assertThat(models.getFirst().provider()).isEqualTo("openai");
    assertThat(models.getFirst().name()).isEqualTo("deepseek-v4-flash");
  }
}
