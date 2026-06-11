package com.ai.domain.event;

import com.ai.domain.vo.ChatSessionId;
import com.ai.domain.vo.MessageId;

import java.time.Instant;

/**
 * Chat response generated event.
 * Published when AI generates a response.
 */
public final class ChatResponseGeneratedEvent extends DomainEvent {

    private final ChatSessionId sessionId;
    private final MessageId userMessageId;
    private final MessageId responseMessageId;
    private final String responseContent;

    public ChatResponseGeneratedEvent(ChatSessionId sessionId, MessageId userMessageId,
                                       MessageId responseMessageId, String responseContent,
                                       Instant occurredAt) {
        super(occurredAt);
        this.sessionId = sessionId;
        this.userMessageId = userMessageId;
        this.responseMessageId = responseMessageId;
        this.responseContent = responseContent;
    }

    public ChatSessionId getSessionId() {
        return sessionId;
    }

    public MessageId getUserMessageId() {
        return userMessageId;
    }

    public MessageId getResponseMessageId() {
        return responseMessageId;
    }

    public String getResponseContent() {
        return responseContent;
    }

    @Override
    public String getEventType() {
        return "ChatResponseGenerated";
    }

    @Override
    public String toString() {
        return String.format("ChatResponseGeneratedEvent{sessionId=%s, userMessageId=%s, responseMessageId=%s, occurredAt=%s}",
            sessionId, userMessageId, responseMessageId, getOccurredAt());
    }
}
