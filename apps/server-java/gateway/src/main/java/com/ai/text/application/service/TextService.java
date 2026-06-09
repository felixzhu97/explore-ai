package com.ai.text.application.service;

import com.ai.text.presentation.dto.ChatMessage;
import com.ai.text.presentation.dto.ChatRequest;
import com.ai.text.presentation.dto.ChatResponse;
import com.ai.text.presentation.dto.ModelInfo;
import com.ai.text.presentation.dto.ProviderInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Application service for text/chat operations.
 * Uses LangChain4j for LLM integration.
 */
@Service
public class TextService {

    private static final Logger log = LoggerFactory.getLogger(TextService.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Value("${langchain4j.chat.model-name:deepseek-v4-flash}")
    private String defaultModel;

    public TextService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    /**
     * Get list of available providers.
     */
    public List<ProviderInfo> getProviders() {
        return List.of(
                ProviderInfo.of("openai", "OpenAI", List.of(
                        "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-3.5-turbo"
                )),
                ProviderInfo.of("anthropic", "Anthropic", List.of(
                        "claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022", "claude-3-opus-20240229"
                )),
                ProviderInfo.of("ollama", "Ollama (Local)", List.of(
                        "llama3.2", "qwen2.5", "mistral", "codellama"
                )),
                ProviderInfo.of("deepseek", "DeepSeek", List.of(
                        "deepseek-v4-flash", "deepseek-chat"
                ))
        );
    }

    /**
     * Get available models for a provider.
     */
    public List<ModelInfo> getModels(String provider) {
        if (provider == null || provider.isBlank()) {
            provider = "openai";
        }
        provider = provider.toLowerCase().trim();

        return switch (provider) {
            case "openai" -> List.of(
                    ModelInfo.of("gpt-4o", "openai"),
                    ModelInfo.of("gpt-4o-mini", "openai"),
                    ModelInfo.of("gpt-4-turbo", "openai"),
                    ModelInfo.of("gpt-3.5-turbo", "openai")
            );
            case "anthropic" -> List.of(
                    ModelInfo.of("claude-3-5-sonnet-20241022", "anthropic"),
                    ModelInfo.of("claude-3-5-haiku-20241022", "anthropic"),
                    ModelInfo.of("claude-3-opus-20240229", "anthropic")
            );
            case "ollama" -> List.of(
                    ModelInfo.of("llama3.2", "ollama"),
                    ModelInfo.of("qwen2.5", "ollama"),
                    ModelInfo.of("mistral", "ollama"),
                    ModelInfo.of("codellama", "ollama")
            );
            case "deepseek" -> List.of(
                    ModelInfo.of("deepseek-v4-flash", "deepseek"),
                    ModelInfo.of("deepseek-chat", "deepseek")
            );
            default -> List.of(
                    ModelInfo.of(defaultModel, provider)
            );
        };
    }

    /**
     * Convert DTO messages to LangChain4j messages.
     */
    private List<dev.langchain4j.data.message.ChatMessage> convertMessages(ChatRequest request) {
        List<dev.langchain4j.data.message.ChatMessage> messages = new java.util.ArrayList<>();

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(dev.langchain4j.data.message.SystemMessage.from(request.systemPrompt()));
        }

        for (ChatMessage msg : request.messages()) {
            messages.add(switch (msg.role().toLowerCase()) {
                case "user" -> dev.langchain4j.data.message.UserMessage.from(msg.content());
                case "assistant" -> dev.langchain4j.data.message.AiMessage.from(msg.content());
                case "system" -> dev.langchain4j.data.message.SystemMessage.from(msg.content());
                default -> dev.langchain4j.data.message.UserMessage.from(msg.content());
            });
        }

        return messages;
    }

    /**
     * Perform non-streaming chat completion.
     */
    public Mono<ChatResponse> chat(ChatRequest request) {
        return Mono.fromCallable(() -> {
            String provider = request.effectiveProvider();
            String model = request.effectiveModel();

            log.info("Processing chat request: provider={}, model={}", provider, model);

            List<dev.langchain4j.data.message.ChatMessage> langChainMessages = convertMessages(request);
            dev.langchain4j.model.chat.response.ChatResponse response = chatModel.chat(langChainMessages);

            String text = response.aiMessage().text();
            return ChatResponse.of(text, provider, model, request.sessionId());
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
          .doOnSuccess(resp -> log.info("Chat completed: length={}", resp.text().length()))
          .doOnError(e -> log.error("Chat failed: {}", e.getMessage(), e));
    }

    /**
     * Perform streaming chat completion.
     * Since ChatModel doesn't support streaming directly, we simulate it
     * by returning chunks of the complete response.
     * Emits SSE events with token chunks.
     */
    public Flux<SseEvent> streamChat(ChatRequest request) {
        return Flux.<SseEvent>create(emitter -> {
            String provider = request.effectiveProvider();
            String model = request.effectiveModel();

            log.info("Starting streaming chat: provider={}, model={}", provider, model);

            try {
                List<dev.langchain4j.data.message.ChatMessage> langChainMessages = convertMessages(request);
                dev.langchain4j.model.chat.response.ChatResponse response = chatModel.chat(langChainMessages);
                String fullResponse = response.aiMessage().text();

                // Emit chunks of the response for streaming simulation
                int chunkSize = 4;
                int index = 0;
                while (index < fullResponse.length()) {
                    int end = Math.min(index + chunkSize, fullResponse.length());
                    String chunk = fullResponse.substring(index, end);
                    String json = objectMapper.writeValueAsString(Map.of("token", chunk));
                    emitter.next(new SseEvent("meta", json));
                    index = end;
                }

                emitter.next(new SseEvent("done", "[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Streaming chat error: {}", e.getMessage(), e);
                try {
                    String json = objectMapper.writeValueAsString(Map.of("error", e.getMessage()));
                    emitter.next(new SseEvent("error", json));
                } catch (Exception ex) {
                    // ignore serialization errors
                }
                emitter.complete();
            }
        });
    }

    /**
     * Check if the text service is healthy.
     */
    public boolean isHealthy() {
        try {
            chatModel.chat(List.of(
                    dev.langchain4j.data.message.UserMessage.from("ping")
            ));
            return true;
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * SSE event wrapper for streaming responses.
     */
    public record SseEvent(String event, String data) {}
}
