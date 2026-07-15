package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.TextChatOptions;
import com.ai.chat.application.usecase.TextProviderCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatModelResolver")
class ChatModelResolverTest {

    @Mock
    private OpenAiChatModel openAiChatModel;

    @Mock
    private ObjectProvider<OllamaChatModel> ollamaChatModel;

    @Mock
    private ObjectProvider<AnthropicChatModel> anthropicChatModel;

    @Mock
    private OllamaChatModel ollamaBean;

    @Mock
    private AnthropicChatModel anthropicBean;

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
                "deepseek-v4-flash",
                "qwen3.5:35b",
                "claude-sonnet-5");
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
    @DisplayName("should reject when provider unavailable")
    void should_rejectWhenProviderUnavailable() {
        when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(false);

        assertThatThrownBy(() -> resolver.resolve(TextChatOptions.of("anthropic", "claude-sonnet-5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    @DisplayName("should reject when ollama bean unavailable")
    void should_rejectWhenOllamaBeanUnavailable() {
        when(providerCatalog.isProviderAvailable("ollama")).thenReturn(true);
        when(ollamaChatModel.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(TextChatOptions.of("ollama", "qwen3.5:35b")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ollama");
    }

    @Test
    @DisplayName("should reject when anthropic bean unavailable")
    void should_rejectWhenAnthropicBeanUnavailable() {
        when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(true);
        when(anthropicChatModel.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve(TextChatOptions.of("anthropic", "claude-sonnet-5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Anthropic");
    }

    @Test
    @DisplayName("should use requested model for openai provider")
    void should_useRequestedModelForOpenAi() {
        when(providerCatalog.isProviderAvailable("openai")).thenReturn(true);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("openai", "deepseek-v4-pro"));

        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("deepseek-v4-pro");
    }

    @Test
    @DisplayName("should resolve ollama with configured default model")
    void should_resolveOllamaWithDefaultModel() {
        when(providerCatalog.isProviderAvailable("ollama")).thenReturn(true);
        when(ollamaChatModel.getIfAvailable()).thenReturn(ollamaBean);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("ollama", null));

        assertThat(resolved.provider()).isEqualTo("ollama");
        assertThat(resolved.chatModel()).isSameAs(ollamaBean);
        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("qwen3.5:35b");
    }

    @Test
    @DisplayName("should resolve anthropic with configured default model")
    void should_resolveAnthropicWithDefaultModel() {
        when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(true);
        when(anthropicChatModel.getIfAvailable()).thenReturn(anthropicBean);

        ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("anthropic", null));

        assertThat(resolved.provider()).isEqualTo("anthropic");
        assertThat(resolved.chatModel()).isSameAs(anthropicBean);
        assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("claude-sonnet-5");
    }
}
