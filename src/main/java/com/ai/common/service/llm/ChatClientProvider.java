package com.ai.common.service.llm;

import org.springframework.ai.chat.client.ChatClient;

/** Creates Spring AI chat clients configured for memory, tools, or bare stateless use. */
public interface ChatClientProvider {
  /** Creates a chat client with memory. */
  ChatClient create(TextChatOptions options);

  /** Creates a chat client with memory for the conversation. */
  ChatClient create(TextChatOptions options, String conversationId);

  /** Build a ChatClient for a composition profile (memory / tools / bare). */
  ChatClient create(TextChatOptions options, ChatClientProfile profile, String conversationId);

  /** Creates a chat client without memory. */
  ChatClient createStateless(TextChatOptions options);

  /** Creates a chat client without memory, tools or advisors. */
  ChatClient createBareStateless(TextChatOptions options);
}
