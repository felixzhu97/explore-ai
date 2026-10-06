package com.ai.chat.service;

import com.ai.chat.domain.vo.SessionTitle;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Generates chat session titles from the first exchange in the user's language. */
@Service
@RequiredArgsConstructor
public class SessionTitleGenerator {

  private static final Logger log = LoggerFactory.getLogger(SessionTitleGenerator.class);

  private static final String SYSTEM_PROMPT =
      """
            Generate a short chat title (max %d characters).
            Use the same language as the user's message.
            """
          .formatted(SessionTitle.MAX_DERIVED_LENGTH);

  private final ChatClientProvider chatClientProvider;

  /** Asks the LLM for a short title from the first exchange, falling back to the user message. */
  public SessionTitle generate(String userMessage, String assistantReply) {
    SessionTitle fallback = SessionTitle.fromFirstMessage(userMessage);
    if (fallback.isDefault() || assistantReply == null || assistantReply.isBlank()) {
      return fallback;
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
      if (response != null) {
        SessionTitle generated = SessionTitle.generated(response.title());
        if (!generated.isDefault()) {
          return generated;
        }
      }
    } catch (Exception e) {
      log.warn("Failed to generate session title via LLM, using fallback", e);
    }
    return fallback;
  }

  record SessionTitleResponse(String title) {}
}
