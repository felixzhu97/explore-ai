package com.ai.common.infra.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;

/** Documentation. */
public record ResolvedChatModel(
    ChatModel chatModel, ChatOptions.Builder<?> optionsBuilder, String provider) {}
