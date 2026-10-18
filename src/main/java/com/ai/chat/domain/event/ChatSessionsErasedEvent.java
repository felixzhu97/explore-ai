package com.ai.chat.domain.event;

import java.util.List;

/**
 * Chat sessions erased event.
 *
 * @param sessionIds ids of the erased chat sessions
 */
public record ChatSessionsErasedEvent(List<String> sessionIds) {}
