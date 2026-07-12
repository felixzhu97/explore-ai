package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.TextChatOptions;
import com.ai.chat.application.usecase.TextProviderCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ChatModelResolver.class);

    private final ChatModel defaultChatModel;
    private final ObjectProvider<OllamaChatModel> ollamaChatModel;
    private final ObjectProvider<AnthropicChatModel> anthropicChatModel;
    private final TextProviderCatalog providerCatalog;
    private final String defaultOpenAiModel;

    public ChatModelResolver(
            ChatModel defaultChatModel,
            ObjectProvider<OllamaChatModel> ollamaChatModel,
            ObjectProvider<AnthropicChatModel> anthropicChatModel,
            TextProviderCatalog providerCatalog,
            @Value("${spring.ai.openai.chat.model:deepseek-v4-flash}") String defaultOpenAiModel) {
        this.defaultChatModel = defaultChatModel;
        this.ollamaChatModel = ollamaChatModel;
        this.anthropicChatModel = anthropicChatModel;
        this.providerCatalog = providerCatalog;
        this.defaultOpenAiModel = defaultOpenAiModel;
    }

    public ResolvedChatModel resolve(TextChatOptions options) {
        String provider = options.provider();
        if (!providerCatalog.isProviderAvailable(provider)) {
            log.warn("Provider '{}' unavailable, falling back to openai", provider);
            return resolveOpenAi(null);
        }

        return switch (provider) {
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
            log.warn("Ollama chat model not configured, falling back to openai");
            return resolveOpenAi(null);
        }
        String effectiveModel = model == null || model.isBlank() ? "qwen3.5" : model;
        ChatOptions.Builder<?> chatOptions = OllamaChatOptions.builder()
                .model(effectiveModel);
        return new ResolvedChatModel(ollama, chatOptions, "ollama");
    }

    private ResolvedChatModel resolveAnthropic(String model) {
        AnthropicChatModel anthropic = anthropicChatModel.getIfAvailable();
        if (anthropic == null) {
            log.warn("Anthropic chat model not configured, falling back to openai");
            return resolveOpenAi(null);
        }
        String effectiveModel = model == null || model.isBlank() ? "claude-3-5-sonnet-20241022" : model;
        ChatOptions.Builder<?> chatOptions = AnthropicChatOptions.builder()
                .model(effectiveModel);
        return new ResolvedChatModel(anthropic, chatOptions, "anthropic");
    }
}
