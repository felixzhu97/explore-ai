package com.ai.common.application.llm;

import org.springframework.ai.chat.client.ChatClient;

public interface ChatClientProvider {

    ChatClient create(TextChatOptions options);

    ChatClient create(TextChatOptions options, String conversationId);

    ChatClient createStateless(TextChatOptions options);

    ChatClient createBareStateless(TextChatOptions options);
}
