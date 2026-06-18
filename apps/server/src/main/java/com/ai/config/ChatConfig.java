package com.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ChatClient configuration.
 * Specifies which ChatModel to use when multiple models are available.
 */
@Configuration
public class ChatConfig {

    @Bean
    @Primary
    public ChatModel primaryChatModel(
            org.springframework.ai.openai.OpenAiChatModel openAiChatModel) {
        return openAiChatModel;
    }
}
