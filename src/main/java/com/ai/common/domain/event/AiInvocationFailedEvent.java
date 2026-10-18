package com.ai.common.domain.event;

import com.ai.common.domain.model.OwnerKey;

/**
 * AI invocation failed event.
 *
 * @param capability capability value such as {@code chat} or {@code rag}
 * @param operation operation name such as {@code chat.stream}
 * @param latencyMs elapsed milliseconds
 * @param owner owner the invocation is attributed to
 * @param provider model provider, or {@code null}
 * @param model model name, or {@code null}
 * @param sessionId chat session id, or {@code null}
 * @param documentId RAG document id, or {@code null}
 * @param agentType agent type, or {@code null}
 * @param errorCode error type name
 * @param errorMessage error message, or {@code null}
 */
public record AiInvocationFailedEvent(
    String capability,
    String operation,
    long latencyMs,
    OwnerKey owner,
    String provider,
    String model,
    String sessionId,
    String documentId,
    String agentType,
    String errorCode,
    String errorMessage) {}
