package com.ai.agents.infrastructure.adapter;

import com.ai.agents.domain.AgentType;
import com.ai.agents.domain.Conversation;
import com.ai.agents.presentation.dto.AgentRequestDto;
import com.ai.agents.presentation.dto.AgentResponseDto;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Text Agent Adapter.
 * Provides text processing capabilities including translation, summarization, and completion.
 */
@Component
public class TextAgentAdapter implements AgentAdapter {

    private static final Logger log = LoggerFactory.getLogger(TextAgentAdapter.class);

    private final ChatModel chatModel;

    @Value("${ai.agents.text.system-prompt:You are a helpful text processing assistant.}")
    private String systemPrompt;

    public TextAgentAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AgentType getType() {
        return AgentType.TEXT;
    }

    @Override
    public Mono<AgentResponseDto> execute(Conversation conversation, AgentRequestDto request) {
        log.info("Text agent processing request");

        Map<String, Object> metadata = request.metadata();
        String operation = metadata != null ? (String) metadata.getOrDefault("operation", "complete") : "complete";

        return switch (operation) {
            case "chat" -> handleChat(request);
            case "complete" -> handleComplete(request);
            case "translate" -> handleTranslate(request);
            case "summarize" -> handleSummarize(request);
            default -> handleComplete(request);
        };
    }

    private Mono<AgentResponseDto> handleChat(AgentRequestDto request) {
        Map<String, Object> metadata = request.metadata();
        List<Map<String, String>> messages = extractMessages(metadata);

        if (messages.isEmpty()) {
            String userMessage = request.getUserMessage();
            messages = userMessage != null ? List.of(Map.of("role", "user", "content", userMessage)) : List.of();
        }

        Double temperature = metadata != null && metadata.containsKey("temperature")
            ? ((Number) metadata.get("temperature")).doubleValue() : 0.7;
        Integer maxTokens = metadata != null && metadata.containsKey("max_tokens")
            ? ((Number) metadata.get("max_tokens")).intValue() : 4096;

        final List<Map<String, String>> finalMessages = messages;
        final double finalTemperature = temperature;
        final int finalMaxTokens = maxTokens;

        return Mono.fromCallable(() -> {
            try {
                String response = chat(finalMessages, finalTemperature, finalMaxTokens);
                return AgentResponseDto.success(response, AgentType.TEXT)
                        .withMetadata(Map.of("operation", "chat"));
            } catch (Exception e) {
                log.error("Text chat failed", e);
                return AgentResponseDto.error("Text chat failed: " + e.getMessage());
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<AgentResponseDto> handleComplete(AgentRequestDto request) {
        Map<String, Object> metadata = request.metadata();
        String systemPromptToUse = metadata != null 
            ? (String) metadata.getOrDefault("system_prompt", systemPrompt) 
            : systemPrompt;

        return Mono.fromCallable(() -> {
            try {
                String userMessage = request.getUserMessage();
                dev.langchain4j.data.message.ChatMessage systemMsg = SystemMessage.from(systemPromptToUse);
                dev.langchain4j.data.message.ChatMessage userMsg = UserMessage.from(userMessage);

                ChatResponse response = chatModel.chat(List.of(systemMsg, userMsg));
                String text = response.aiMessage().text();

                return AgentResponseDto.success(text, AgentType.TEXT)
                        .withMetadata(Map.of("operation", "complete"));
            } catch (Exception e) {
                log.error("Text completion failed", e);
                return AgentResponseDto.error("Text completion failed: " + e.getMessage());
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<AgentResponseDto> handleTranslate(AgentRequestDto request) {
        Map<String, Object> metadata = request.metadata();
        String targetLanguage = metadata != null ? (String) metadata.get("target_language") : "English";
        String userMessage = request.getUserMessage() != null ? request.getUserMessage() : "";

        String translatePrompt = String.format(
                "Translate the following text to %s. Only output the translation, nothing else.\n\nText: %s",
                targetLanguage,
                userMessage
        );

        return Mono.fromCallable(() -> {
            try {
                dev.langchain4j.data.message.ChatMessage userMsg = UserMessage.from(translatePrompt);
                ChatResponse response = chatModel.chat(List.of(userMsg));
                String translation = response.aiMessage().text();

                return AgentResponseDto.success(translation, AgentType.TEXT)
                        .withMetadata(Map.of("operation", "translate", "target_language", targetLanguage));
            } catch (Exception e) {
                log.error("Translation failed", e);
                return AgentResponseDto.error("Translation failed: " + e.getMessage());
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private Mono<AgentResponseDto> handleSummarize(AgentRequestDto request) {
        String userMessage = request.getUserMessage() != null ? request.getUserMessage() : "";
        String summarizePrompt = String.format(
                "Summarize the following text concisely:\n\n%s",
                userMessage
        );

        return Mono.fromCallable(() -> {
            try {
                dev.langchain4j.data.message.ChatMessage userMsg = UserMessage.from(summarizePrompt);
                ChatResponse response = chatModel.chat(List.of(userMsg));
                String summary = response.aiMessage().text();

                return AgentResponseDto.success(summary, AgentType.TEXT)
                        .withMetadata(Map.of("operation", "summarize"));
            } catch (Exception e) {
                log.error("Summarization failed", e);
                return AgentResponseDto.error("Summarization failed: " + e.getMessage());
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private String chat(List<Map<String, String>> messages, double temperature, int maxTokens) {
        List<dev.langchain4j.data.message.ChatMessage> langchainMessages = messages.stream()
                .map(m -> {
                    String role = m.get("role");
                    String content = m.get("content");
                    if ("system".equalsIgnoreCase(role)) {
                        return SystemMessage.from(content);
                    } else if ("user".equalsIgnoreCase(role)) {
                        return UserMessage.from(content);
                    } else if ("assistant".equalsIgnoreCase(role)) {
                        return AiMessage.from(content);
                    }
                    return UserMessage.from(content);
                })
                .toList();

        ChatResponse response = chatModel.chat(langchainMessages);
        return response.aiMessage().text();
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

    @Override
    public boolean isAvailable() {
        return chatModel != null;
    }
}
