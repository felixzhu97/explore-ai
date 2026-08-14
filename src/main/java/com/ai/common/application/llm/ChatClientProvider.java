package com.ai.common.application.llm;

import org.springframework.ai.chat.client.ChatClient;

/** Documentation. */
public interface ChatClientProvider {
  /** Documentation. */
  ChatClient create(TextChatOptions options);

  /** Documentation. */
  ChatClient create(TextChatOptions options, String conversationId);

  /** Build a ChatClient for a composition profile (memory / tools / bare). */
  ChatClient create(TextChatOptions options, ChatClientProfile profile, String conversationId);

  /** Documentation. */
  ChatClient createStateless(TextChatOptions options);

  /** Documentation. */
  ChatClient createBareStateless(TextChatOptions options);
}
