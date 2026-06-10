package com.ai.domain.event;

import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;

import java.time.Instant;

/**
 * Chat message received event.
 * Published when user sends a message.
 */
public final class ChatMessageReceivedEvent extends DomainEvent {

    private final ChatSessionId sessionId;
    private final MessageId messageId;
    private final String content;

    public ChatMessageReceivedEvent(ChatSessionId sessionId, MessageId messageId,
                                    String content, Instant occurredAt) {
        super(occurredAt);
        this.sessionId = sessionId;
        this.messageId = messageId;
        this.content = content;
    }

    public ChatSessionId getSessionId() {
        return sessionId;
    }

    public MessageId getMessageId() {
        return messageId;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String getEventType() {
        return "ChatMessageReceived";
    }

    @Override
    public String toString() {
        return String.format("ChatMessageReceivedEvent{sessionId=%s, messageId=%s, occurredAt=%s}",
            sessionId, messageId, getOccurredAt());
    }
}
