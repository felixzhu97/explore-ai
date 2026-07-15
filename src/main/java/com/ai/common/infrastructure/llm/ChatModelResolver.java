package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.TextChatOptions;
import com.ai.chat.application.usecase.TextProviderCatalog;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChatModelResolver {

    private final ChatModel defaultChatModel;
    private final ObjectProvider<OllamaChatModel> ollamaChatModel;
    private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
    private final TextProviderCatalog providerCatalog;
    private final String defaultOpenAiModel;
    private final String defaultOllamaModel;
    private final String defaultAnthropicModel;

    public ChatModelResolver(
            ChatModel defaultChatModel,
            ObjectProvider<OllamaChatModel> ollamaChatModel,
            ObjectProvider<AnthropicChatModel> anthropicChatModel,
            TextProviderCatalog providerCatalog,
            @Value("${spring.ai.openai.chat.model:deepseek-v4-flash}") String defaultOpenAiModel,
            @Value("${spring.ai.ollama.chat.model:qwen3.5:35b}") String defaultOllamaModel,
            @Value("${spring.ai.anthropic.chat.options.model:claude-sonnet-5}") String defaultAnthropicModel) {
        this.defaultChatModel = defaultChatModel;
        this.ollamaChatModel = ollamaChatModel;
        this.anthropicChatModel = anthropicChatModel;
        this.providerCatalog = providerCatalog;
        this.defaultOpenAiModel = defaultOpenAiModel;
        this.defaultOllamaModel = defaultOllamaModel;
        this.defaultAnthropicModel = defaultAnthropicModel;
    }

    public ResolvedChatModel resolve(TextChatOptions options) {
        String provider = options.provider();
        if (!providerCatalog.isProviderAvailable(provider)) {
            throw new IllegalArgumentException(
                    "Provider '%s' is not configured. Configure the API key or enable the provider before chatting."
                            .formatted(provider == null || provider.isBlank() ? "unknown" : provider));
        }

        return switch (provider == null || provider.isBlank() ? "openai" : provider.toLowerCase()) {
            case "ollama" -> resolveOllama(options.model());
            case "anthropic" -> resolveAnthropic(options.model());
            default -> resolveOpenAi(options.model());
        };
    }

    private ResolvedChatModel resolveOpenAi(String model) {
        ChatModel chatModel = defaultChatModel instanceof OpenAiChatModel openAi
                ? openAi
                : defaultChatModel;
        String effectiveModel = model == null || model.isBlank() ? defaultOpenAiModel : model;
        ChatOptions.Builder<?> chatOptions = OpenAiChatOptions.builder()
                .model(effectiveModel);
        return new ResolvedChatModel(chatModel, chatOptions, "openai");
    }

    private ResolvedChatModel resolveOllama(String model) {
        OllamaChatModel ollama = ollamaChatModel.getIfAvailable();
        if (ollama == null) {
            throw new IllegalArgumentException(
                    "Ollama chat model is not configured. Enable spring.ai.ollama.chat.enabled and ensure Ollama is reachable.");
        }
        String effectiveModel = model == null || model.isBlank() ? defaultOllamaModel : model;
        ChatOptions.Builder<?> chatOptions = OllamaChatOptions.builder()
                .model(effectiveModel);
        return new ResolvedChatModel(ollama, chatOptions, "ollama");
    }

    private ResolvedChatModel resolveAnthropic(String model) {
        AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
        if (anthropic == null) {
            throw new IllegalArgumentException(
                    "Anthropic chat model is not configured. Set ANTHROPIC_API_KEY before chatting.");
        }
        String effectiveModel = model == null || model.isBlank() ? defaultAnthropicModel : model;
        ChatOptions.Builder<?> chatOptions = AnthropicChatOptions.builder()
                .model(effectiveModel);
        return new ResolvedChatModel(anthropic, chatOptions, "anthropic");
    }
}
