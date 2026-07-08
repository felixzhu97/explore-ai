package com.ai.chat.application.usecase;

import com.ai.chat.web.dto.ModelInfoResponse;
import com.ai.chat.web.dto.ProviderInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TextProviderCatalog {

    private static final Map<String, List<ModelInfoResponse>> MODELS_BY_PROVIDER = Map.of(
            "openai", List.of(
                    new ModelInfoResponse("deepseek-v4-flash", "openai", "DeepSeek via OpenAI-compatible API"),
                    new ModelInfoResponse("gpt-4o-mini", "openai", "Alias for configured chat model"),
                    new ModelInfoResponse("gpt-4o", "openai", "Alias for configured chat model")
            ),
            "anthropic", List.of(
                    new ModelInfoResponse("claude-3-5-sonnet", "anthropic", "Not configured on this deployment"),
                    new ModelInfoResponse("claude-3-opus", "anthropic", "Not configured on this deployment"),
                    new ModelInfoResponse("claude-3-haiku", "anthropic", "Not configured on this deployment")
            ),
            "ollama", List.of(
                    new ModelInfoResponse("qwen3.5", "ollama", "Local Ollama multimodal model"),
                    new ModelInfoResponse("llama3", "ollama", "Local Ollama chat model")
            )
    );

    private final String configuredChatModel;
    private final boolean ollamaChatEnabled;

    public TextProviderCatalog(
            @Value("${spring.ai.openai.chat.model:deepseek-v4-flash}") String configuredChatModel,
            @Value("${spring.ai.ollama.chat.enabled:false}") boolean ollamaChatEnabled) {
        this.configuredChatModel = configuredChatModel;
        this.ollamaChatEnabled = ollamaChatEnabled;
    }

    public List<ProviderInfoResponse> listProviders() {
        return List.of(
                new ProviderInfoResponse(
                        "openai",
                        "DeepSeek",
                        modelNames("openai"),
                        "available"
                ),
                new ProviderInfoResponse(
                        "anthropic",
                        "Anthropic Claude",
                        modelNames("anthropic"),
                        "unavailable"
                ),
                new ProviderInfoResponse(
                        "ollama",
                        "Ollama (Local)",
                        modelNames("ollama"),
                        ollamaChatEnabled ? "available" : "unavailable"
                )
        );
    }

    public List<ModelInfoResponse> listModels(String provider) {
        String key = provider == null || provider.isBlank() ? "openai" : provider.toLowerCase();
        List<ModelInfoResponse> models = MODELS_BY_PROVIDER.getOrDefault(key, MODELS_BY_PROVIDER.get("openai"));

        if ("openai".equals(key)) {
            return models.stream()
                    .map(model -> primaryModel(model))
                    .toList();
        }

        return models;
    }

    private ModelInfoResponse primaryModel(ModelInfoResponse model) {
        if ("deepseek-v4-flash".equals(model.name())) {
            return new ModelInfoResponse(configuredChatModel, model.provider(), model.description());
        }
        return model;
    }

    private List<String> modelNames(String provider) {
        return listModels(provider).stream().map(ModelInfoResponse::name).toList();
    }
}
