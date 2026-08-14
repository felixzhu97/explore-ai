package com.ai.common.infrastructure.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

/** Documentation. */
public record ResolvedChatModel(
    ChatModel chatModel, ChatOptions.Builder<?> optionsBuilder, String provider) {}
