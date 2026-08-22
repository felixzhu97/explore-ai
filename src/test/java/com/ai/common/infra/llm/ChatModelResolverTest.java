package com.ai.common.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ai.chat.service.usecase.TextProviderCatalog;
import com.ai.common.service.llm.TextChatOptions;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatModelResolver")
class ChatModelResolverTest {

  @Mock private OpenAiChatModel openAiChatModel;

  @Mock private ObjectProvider<OllamaChatModel> ollamaChatModel;

  @Mock private ObjectProvider<AnthropicChatModel> anthropicChatModel;

  @Mock private OllamaChatModel ollamaBean;

  @Mock private AnthropicChatModel anthropicBean;

  @Mock private TextProviderCatalog providerCatalog;

  private ChatModelResolver resolver;

  @BeforeEach
  void setUp() {
    resolver =
        new ChatModelResolver(
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
  void shouldResolveOpenAiWithDefaultModel() {
    when(providerCatalog.isProviderAvailable("openai")).thenReturn(true);

    ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("openai", null));

    assertThat(resolved.provider()).isEqualTo("openai");
    assertThat(resolved.chatModel()).isSameAs(openAiChatModel);
    assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("deepseek-v4-flash");
  }

  @Test
  @DisplayName("should reject when provider unavailable")
  void shouldRejectWhenProviderUnavailable() {
    when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(false);

    assertThatThrownBy(() -> resolver.resolve(TextChatOptions.of("anthropic", "claude-sonnet-5")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not configured");
  }

  @Test
  @DisplayName("should reject when ollama bean unavailable")
  void shouldRejectWhenOllamaBeanUnavailable() {
    when(providerCatalog.isProviderAvailable("ollama")).thenReturn(true);
    when(ollamaChatModel.getIfAvailable()).thenReturn(null);

    assertThatThrownBy(() -> resolver.resolve(TextChatOptions.of("ollama", "qwen3.5:35b")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Ollama");
  }

  @Test
  @DisplayName("should reject when anthropic bean unavailable")
  void shouldRejectWhenAnthropicBeanUnavailable() {
    when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(true);
    when(anthropicChatModel.getIfAvailable()).thenReturn(null);

    assertThatThrownBy(() -> resolver.resolve(TextChatOptions.of("anthropic", "claude-sonnet-5")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Anthropic");
  }

  @Test
  @DisplayName("should use requested model for openai provider")
  void shouldUseRequestedModelForOpenAi() {
    when(providerCatalog.isProviderAvailable("openai")).thenReturn(true);

    ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("openai", "deepseek-v4-pro"));

    assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("deepseek-v4-pro");
  }

  @Test
  @DisplayName("should resolve ollama with configured default model")
  void shouldResolveOllamaWithDefaultModel() {
    when(providerCatalog.isProviderAvailable("ollama")).thenReturn(true);
    when(ollamaChatModel.getIfAvailable()).thenReturn(ollamaBean);

    ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("ollama", null));

    assertThat(resolved.provider()).isEqualTo("ollama");
    assertThat(resolved.chatModel()).isSameAs(ollamaBean);
    assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("qwen3.5:35b");
  }

  @Test
  @DisplayName("should resolve anthropic with configured default model")
  void shouldResolveAnthropicWithDefaultModel() {
    when(providerCatalog.isProviderAvailable("anthropic")).thenReturn(true);
    when(anthropicChatModel.getIfAvailable()).thenReturn(anthropicBean);

    ResolvedChatModel resolved = resolver.resolve(TextChatOptions.of("anthropic", null));

    assertThat(resolved.provider()).isEqualTo("anthropic");
    assertThat(resolved.chatModel()).isSameAs(anthropicBean);
    assertThat(resolved.optionsBuilder().build().getModel()).isEqualTo("claude-sonnet-5");
  }
}
