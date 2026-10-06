package com.ai.chat.service;

import com.ai.chat.domain.model.ChatSession;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Generates chat session titles of at most 50 characters in the user's language. */
@Service
@RequiredArgsConstructor
public class SessionTitleGenerator {

  private static final Logger log = LoggerFactory.getLogger(SessionTitleGenerator.class);
  private static final int MAX_TITLE_LENGTH = 50;

  private static final String SYSTEM_PROMPT =
      """
            Generate a short chat title (max 50 characters).
            Use the same language as the user's message.
            """;

  private final ChatClientProvider chatClientProvider;

  /** Asks the LLM for a short title from the first exchange, falling back to the user message. */
  public String generate(String userMessage, String assistantReply) {
    if (userMessage == null
        || userMessage.isBlank()
        || assistantReply == null
        || assistantReply.isBlank()) {
      return buildFallbackTitle(userMessage);
    }
    try {
      ChatClient chatClient = chatClientProvider.createStateless(TextChatOptions.withoutTools());
      SessionTitleResponse response =
          chatClient
              .prompt()
              .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
              .system(SYSTEM_PROMPT)
              .user("User: %s%nAssistant: %s".formatted(userMessage, assistantReply))
              .call()
              .entity(SessionTitleResponse.class, spec -> spec.validateSchema());
      if (response != null && response.title() != null && !response.title().isBlank()) {
        return sanitize(response.title());
      }
    } catch (Exception e) {
      log.warn("Failed to generate session title via LLM, using fallback", e);
    }
    return buildFallbackTitle(userMessage);
  }

  record SessionTitleResponse(String title) {}

  /** Builds a short title from the user message. */
  String buildFallbackTitle(String userMessage) {
    if (userMessage == null || userMessage.isBlank()) {
      return ChatSession.DEFAULT_TITLE;
    }
    String cleaned = userMessage.replaceAll("\\s+", " ").trim();
    if (cleaned.length() <= MAX_TITLE_LENGTH) {
      return cleaned;
    }
    return cleaned.substring(0, MAX_TITLE_LENGTH);
  }

  private String sanitize(String title) {
    String cleaned = title.strip().replaceAll("^[\"']+|[\"']+$", "");
    if (cleaned.length() <= MAX_TITLE_LENGTH) {
      return cleaned;
    }
    return cleaned.substring(0, MAX_TITLE_LENGTH);
  }
}
