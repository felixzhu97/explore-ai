package com.ai.chat.infrastructure;

import com.ai.common.infrastructure.ToolCallMarkupFilter;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Drops or strips DeepSeek DSML tool markup from assistant messages so ChatMemory
 * never retains phantom tool-call text after {@code toolChoice=none}.
 */
public final class SanitizingChatMemory implements ChatMemory {

    private final ChatMemory delegate;

    public SanitizingChatMemory(ChatMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<Message> cleaned = sanitizeMessages(messages);
        if (!cleaned.isEmpty()) {
            delegate.add(conversationId, cleaned);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        return sanitizeMessages(delegate.get(conversationId));
    }

    @Override
    public void clear(String conversationId) {
        delegate.clear(conversationId);
    }

    private static List<Message> sanitizeMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        List<Message> cleaned = new ArrayList<>(messages.size());
        for (Message message : messages) {
            if (message.getMessageType() != MessageType.ASSISTANT) {
                cleaned.add(message);
                continue;
            }
            String text = message.getText() == null ? "" : message.getText();
            if (!ToolCallMarkupFilter.looksLikeToolMarkup(text)) {
                cleaned.add(message);
                continue;
            }
            String sanitized = ToolCallMarkupFilter.sanitize(text);
            if (sanitized.isBlank()) {
                continue;
            }
            cleaned.add(new AssistantMessage(sanitized));
        }
        return List.copyOf(cleaned);
    }
}
