package com.ai.eval.infra.config;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentation. */
@Configuration
public class EvalConfig {
  /** Documentation. */
  @Bean
  public ChatClient evaluationChatClient(ChatClientProvider chatClientProvider) {
    return chatClientProvider.createBareStateless(TextChatOptions.withoutTools());
  }

  /** Documentation. */
  @Bean
  public RelevancyEvaluator relevancyEvaluator(ChatModel chatModel) {
    return new RelevancyEvaluator(ChatClient.builder(chatModel));
  }

  /** Documentation. */
  @Bean
  public FactCheckingEvaluator factCheckingEvaluator(ChatModel chatModel) {
    return FactCheckingEvaluator.builder(ChatClient.builder(chatModel)).build();
  }
}
