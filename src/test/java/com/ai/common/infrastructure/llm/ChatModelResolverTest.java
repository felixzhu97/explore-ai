package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.TextChatOptions;
import com.ai.chat.application.usecase.TextProviderCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatModelResolver")
class ChatModelResolverTest {

    @Mock
    private OpenAiChatModel openAiChatModel;

    @Mock
    private ObjectProvider<org.springframework.ai.ollama.OllamaChatModel> ollamaChatModel;

    @Mock
    private ObjectProvider<org.springframework.ai.anthropic.AnthropicChatModel> anthropicChatModel;

    @Mock
    private TextProviderCatalog providerCatalog;

    private ChatModelResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ChatModelResolver(
                openAiChatModel,
                ollamaChatModel,
                anthropicChatModel,
                providerCatalog,
                "deepseek-v4-flash");
    }

    @Test
    @DisplayName("should resolve openai provider with configured default model")
    void should_resolveOpenAiWithDefaultModel() {
        when(providerCatalog.isProviderAvailable("openai")).thenReturn(true);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("openai", null));

        assertThat(resolved.provider()).isEqualTo("openai");
        assertThat(resolved.chatModel()).isSameAs(openAiChatModel);
        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    @DisplayName("should fall back to openai when provider unavailable")
    void should_fallbackWhenProviderUnavailable() {
        when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(false);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("anthropic", "claude-3-5-sonnet"));

        assertThat(resolved.provider()).isEqualTo("openai");
        assertThat(resolved.chatModel()).isSameAs(openAiChatModel);
        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    @DisplayName("should fall back to default openai model when ollama bean unavailable")
    void should_fallbackToDefaultOpenAiModelWhenOllamaUnavailable() {
        when(providerCatalog.isProviderAvailable("ollama")).thenReturn(true);
        when(ollamaChatModel.getIfAvailable()).thenReturn(null);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("ollama", "qwen3.5"));

        assertThat(resolved.provider()).isEqualTo("openai");
        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    @DisplayName("should fall back to default openai model when anthropic bean unavailable")
    void should_fallbackToDefaultOpenAiModelWhenAnthropicUnavailable() {
        when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(true);
        when(anthropicChatModel.getIfAvailable()).thenReturn(null);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("anthropic", "claude-3-5-sonnet"));

        assertThat(resolved.provider()).isEqualTo("openai");
        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    @DisplayName("should use requested model for openai provider")
    void should_useRequestedModelForOpenAi() {
        when(providerCatalog.isProviderAvailable("openai")).thenReturn(true);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("openai", "gpt-4o"));

        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("gpt-4o");
    }
}
