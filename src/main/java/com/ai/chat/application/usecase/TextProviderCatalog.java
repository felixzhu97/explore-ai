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
                    new ModelInfoResponse("deepseek-v4-flash", "openai", "DeepSeek V4 Flash"),
                    new ModelInfoResponse("deepseek-v4-pro", "openai", "DeepSeek V4 Pro")
            ),
            "anthropic", List.of(
                    new ModelInfoResponse("claude-fable-5", "anthropic", "Claude Fable 5"),
                    new ModelInfoResponse("claude-opus-4-8", "anthropic", "Claude Opus 4.8"),
                    new ModelInfoResponse("claude-sonnet-5", "anthropic", "Claude Sonnet 5"),
                    new ModelInfoResponse("claude-haiku-4-5-20251001", "anthropic", "Claude Haiku 4.5"),
                    new ModelInfoResponse("claude-opus-4-7", "anthropic", "Claude Opus 4.7"),
                    new ModelInfoResponse("claude-opus-4-6", "anthropic", "Claude Opus 4.6"),
                    new ModelInfoResponse("claude-sonnet-4-6", "anthropic", "Claude Sonnet 4.6"),
                    new ModelInfoResponse("claude-sonnet-4-5-20250929", "anthropic", "Claude Sonnet 4.5"),
                    new ModelInfoResponse("claude-opus-4-5-20251101", "anthropic", "Claude Opus 4.5")
            ),
            "ollama", List.of(
                    new ModelInfoResponse("qwen3.5:35b", "ollama", "Qwen 3.5 35B"),
                    new ModelInfoResponse("qwen3:8b", "ollama", "Qwen 3 8B"),
                    new ModelInfoResponse("qwen3:14b", "ollama", "Qwen 3 14B"),
                    new ModelInfoResponse("llama3.2", "ollama", "Llama 3.2"),
                    new ModelInfoResponse("llama3.1:8b", "ollama", "Llama 3.1 8B"),
                    new ModelInfoResponse("gemma3:12b", "ollama", "Gemma 3 12B"),
                    new ModelInfoResponse("mistral", "ollama", "Mistral"),
                    new ModelInfoResponse("deepseek-r1:14b", "ollama", "DeepSeek R1 14B")
            )
    );

    private final boolean ollamaChatEnabled;
    private final boolean anthropicEnabled;

    public TextProviderCatalog(
            @Value("${spring.ai.ollama.chat.enabled:false}") boolean ollamaChatEnabled,
            @Value("${spring.ai.anthropic.api-key:}") String anthropicApiKey) {
        this.ollamaChatEnabled = ollamaChatEnabled;
        this.anthropicEnabled = anthropicApiKey != null && !anthropicApiKey.isBlank();
    }

    public boolean isProviderAvailable(String provider) {
        if (provider == null || provider.isBlank()) {
            return true;
        }
        return switch (provider.toLowerCase()) {
            case "openai" -> true;
            case "ollama" -> ollamaChatEnabled;
            case "anthropic" -> anthropicEnabled;
            default -> false;
        };
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
                        anthropicEnabled ? "available" : "unavailable"
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
        return MODELS_BY_PROVIDER.getOrDefault(key, MODELS_BY_PROVIDER.get("openai"));
    }

    private List<String> modelNames(String provider) {
        return listModels(provider).stream().map(ModelInfoResponse::name).toList();
    }
}
