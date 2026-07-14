package com.ai.eval.infrastructure.config;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvalConfig {

    @Bean
    public ChatClient evaluationChatClient(ChatClientProvider chatClientProvider) {
        return chatClientProvider.createBareStateless(TextChatOptions.withoutTools());
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
