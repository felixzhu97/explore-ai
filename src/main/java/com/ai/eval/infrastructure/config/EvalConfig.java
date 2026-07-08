package com.ai.eval.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for chat evaluation module.
 * Uses Spring AI Evaluators (RelevancyEvaluator, FactCheckingEvaluator) for RAG-style evaluation.
 * A separate ChatClient instance is used to mitigate model biases.
 */
@Configuration
public class EvalConfig {

    @Bean
    public ChatClient evaluationChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    @Bean
    public RelevancyEvaluator relevancyEvaluator(ChatModel chatModel) {
        return new RelevancyEvaluator(ChatClient.builder(chatModel));
    }

    @Bean
    public FactCheckingEvaluator factCheckingEvaluator(ChatModel chatModel) {
        return FactCheckingEvaluator.builder(ChatClient.builder(chatModel)).build();
    }
}
