package com.ai.chat.application.usecase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextProviderCatalogTest {

    @Test
    void should_list_openai_as_available_provider() {
        var catalog = new TextProviderCatalog("deepseek-v4-flash", false, "");

        var providers = catalog.listProviders();

        assertThat(providers).hasSize(3);
        assertThat(providers.getFirst().name()).isEqualTo("openai");
        assertThat(providers.getFirst().status()).isEqualTo("available");
        assertThat(providers.getFirst().displayName()).isEqualTo("DeepSeek");
    }

    @Test
    void should_return_configured_model_for_openai_provider() {
        var catalog = new TextProviderCatalog("deepseek-v4-flash", false, "");

        var models = catalog.listModels("openai");

        assertThat(models).isNotEmpty();
        assertThat(models.getFirst().name()).isEqualTo("deepseek-v4-flash");
        assertThat(models.getFirst().provider()).isEqualTo("openai");
    }

    @Test
    void should_mark_ollama_unavailable_when_chat_disabled() {
        var catalog = new TextProviderCatalog("deepseek-v4-flash", false, "");

        var providers = catalog.listProviders();

        var ollama = providers.stream()
                .filter(p -> "ollama".equals(p.name()))
                .findFirst()
                .orElseThrow();
        assertThat(ollama.status()).isEqualTo("unavailable");
    }

    @Test
    void should_mark_anthropic_available_when_api_key_configured() {
        var catalog = new TextProviderCatalog("deepseek-v4-flash", false, "sk-ant-test");

        var anthropic = catalog.listProviders().stream()
                .filter(p -> "anthropic".equals(p.name()))
                .findFirst()
                .orElseThrow();

        assertThat(anthropic.status()).isEqualTo("available");
    }

    @Test
    void should_default_to_openai_models_when_provider_missing() {
        var catalog = new TextProviderCatalog("deepseek-v4-flash", false, "");

        var models = catalog.listModels(null);

        assertThat(models.getFirst().provider()).isEqualTo("openai");
    }
}
