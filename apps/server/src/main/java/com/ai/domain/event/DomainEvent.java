package com.ai.domain.event;

import java.time.Instant;

/**
 * Base class for domain events.
 * All domain events should extend this class.
 */
public sealed abstract class DomainEvent permits ChatMessageReceivedEvent, ChatResponseGeneratedEvent {

    private final Instant occurredAt;

    protected DomainEvent(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public abstract String getEventType();
}
