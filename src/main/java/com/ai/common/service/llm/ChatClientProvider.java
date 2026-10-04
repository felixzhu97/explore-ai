package com.ai.common.service.llm;

import org.springframework.ai.chat.client.ChatClient;

/** Creates Spring AI chat clients configured for memory, tools, or bare stateless use. */
public interface ChatClientProvider {
  ChatClient create(TextChatOptions options);

  ChatClient create(TextChatOptions options, String conversationId);

  /** Build a ChatClient for a composition profile (memory / tools / bare). */
  ChatClient create(TextChatOptions options, ChatClientProfile profile, String conversationId);

  ChatClient createStateless(TextChatOptions options);

  ChatClient createBareStateless(TextChatOptions options);
}
