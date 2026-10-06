package com.ai.chat.infra.config;

import com.ai.chat.infra.memory.SanitizingChatMemory;
import com.ai.common.infra.prompt.PromptTemplates;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ChatMemory configuration using Spring AI 2.0 JDBC persistence. Production ChatClient instances
 * are built via ChatClientFactory (profiles).
 */
@Configuration
public class ChatConfig {
  /** Uses the OpenAI-compatible model as the primary chat model. */
  @Bean
  @Primary
  public ChatModel primaryChatModel(@Qualifier("openAiChatModel") ChatModel openAiChatModel) {
    return openAiChatModel;
  }

  /** Stores chat memory in the database. */
  @Bean
  public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
    return JdbcChatMemoryRepository.builder().jdbcTemplate(jdbcTemplate).build();
  }

  /** Keeps the most recent messages of each conversation in memory. */
  @Bean
  @Primary
  public ChatMemory chatMemory(
      ChatMemoryRepository chatMemoryRepository,
      @Value("${app.ai.chat-memory.max-messages:20}") int maxMessages) {
    ChatMemory window =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(maxMessages)
            .build();
    return new SanitizingChatMemory(window);
  }

  /** Loads the prompt templates. */
  @Bean
  public PromptTemplates promptTemplates() {
    return new PromptTemplates();
  }
}
