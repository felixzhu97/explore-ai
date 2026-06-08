package com.ai.gateway.agent;

import com.ai.common.agent.Agent;
import com.ai.common.agent.AgentRequest;
import com.ai.common.agent.AgentResponse;
import com.ai.common.agent.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Text processing agent delegating to text-service (port 8006).
 * Supports completion, chat, and text transformation operations.
 */
@Component
public class TextAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(TextAgent.class);
    private final WebClient webClient;

    public TextAgent(
            WebClient.Builder webClientBuilder,
            @Value("${ai.agent.services.text.base-url}") String textServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(textServiceUrl).build();
    }

    @Override
    public String name() {
        return "TextAgent";
    }

    @Override
    public AgentType type() {
        return AgentType.TEXT;
    }

    @Override
    public Mono<AgentResponse> process(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        String operation = metadata != null ? (String) metadata.getOrDefault("operation", "complete") : "complete";

        return switch (operation) {
            case "chat" -> handleChat(request);
            case "complete" -> handleComplete(request);
            default -> handleComplete(request);
        };
    }

    private Mono<AgentResponse> handleChat(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        List<Map<String, String>> messages = extractMessages(metadata);

        if (messages.isEmpty()) {
            messages = List.of(Map.of("role", "user", "content", request.message()));
        }

        Double temperature = metadata != null && metadata.containsKey("temperature") 
            ? ((Number) metadata.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = metadata != null && metadata.containsKey("max_tokens") 
            ? ((Number) metadata.get("max_tokens")).intValue() : 4096;

        Map<String, Object> body = Map.of(
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens
        );

        return webClient.post()
                .uri("/api/text/chat")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .<AgentResponse>map(response -> {
                    String text = String.valueOf(response.get("text"));
                    String model = String.valueOf(response.get("model"));
                    return AgentResponse.success(text, type())
                            .withMetadata(Map.of("model", model, "operation", "chat"));
                })
                .onErrorResume(e -> {
                    log.error("Text chat failed: {}", e.getMessage());
                    return Mono.just(AgentResponse.error("Text chat failed: " + e.getMessage()));
                });
    }

    private Mono<AgentResponse> handleComplete(AgentRequest request) {
        Map<String, Object> metadata = request.metadata();
        String systemPrompt = metadata != null ? (String) metadata.get("system_prompt") : null;

        Double temperature = metadata != null && metadata.containsKey("temperature") 
            ? ((Number) metadata.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = metadata != null && metadata.containsKey("max_tokens") 
            ? ((Number) metadata.get("max_tokens")).intValue() : 4096;

        Map<String, Object> body = Map.of(
                "prompt", request.message(),
                "system_prompt", systemPrompt != null ? systemPrompt : "",
                "temperature", temperature,
                "max_tokens", maxTokens
        );

        return webClient.post()
                .uri("/api/text/complete")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .<AgentResponse>map(response -> {
                    String text = String.valueOf(response.get("text"));
                    String model = String.valueOf(response.get("model"));
                    return AgentResponse.success(text, type())
                            .withMetadata(Map.of("model", model, "operation", "complete"));
                })
                .onErrorResume(e -> {
                    log.error("Text completion failed: {}", e.getMessage());
                    return Mono.just(AgentResponse.error("Text completion failed: " + e.getMessage()));
                });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractMessages(Map<String, Object> metadata) {
        if (metadata == null || !metadata.containsKey("messages")) {
            return List.of();
        }
        Object messagesObj = metadata.get("messages");
        if (messagesObj instanceof List<?> list) {
            return list.stream()
                    .filter(m -> m instanceof Map)
                    .map(m -> (Map<String, String>) m)
                    .toList();
        }
        return List.of();
    }
}
