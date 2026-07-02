package com.ai.chat.infrastructure.config;

import com.ai.chat.infrastructure.service.PromptTemplates;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ChatClient configuration with Spring AI 2.0 Chat Memory support.
 * Configures ChatMemory and MessageChatMemoryAdvisor for conversation history management.
 */
@Configuration
public class ChatConfig {

    @Bean
    @Primary
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    @Primary
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "rag.ollama.chat.enabled", havingValue = "true")
    public ChatModel ollamaVisionChatModel(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${rag.ollama.chat.model:qwen3.5:35b}") String model) {
        OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "rag.ollama.chat.enabled", havingValue = "false", matchIfMissing = true)
    public ChatModel openAiChatModel(org.springframework.ai.openai.OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
    }

    @Bean
    public PromptTemplates promptTemplates() {
        return new PromptTemplates();
    }
}
