package com.ai.application.usecase;

import com.ai.application.port.AiChatPort;
import com.ai.application.port.ChatSessionRepositoryPort;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.ChatSession;
import com.ai.domain.model.ChatSessionNotFoundException;
import com.ai.domain.vo.ChatSessionId;

/**
 * Send chat message use case.
 * Single responsibility - handles only the core flow of sending messages.
 */
public class SendChatMessageUseCase {

    private final ChatSessionRepositoryPort repositoryPort;
    private final AiChatPort aiChatPort;

    public SendChatMessageUseCase(ChatSessionRepositoryPort repositoryPort, AiChatPort aiChatPort) {
        this.repositoryPort = repositoryPort;
        this.aiChatPort = aiChatPort;
    }

    /**
     * Executes sending a message and getting AI response.
     *
     * @param sessionId session ID
     * @param userMessage user message
     * @return AI response message
     */
    public ChatMessage execute(String sessionId, String userMessage) {
        ChatSession session = repositoryPort.findById(ChatSessionId.of(sessionId))
            .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));

        session.addUserMessage(userMessage);
        repositoryPort.save(session);

        String aiResponse = aiChatPort.chat(userMessage);

        ChatMessage assistantMessage = session.addAssistantMessage(aiResponse);
        repositoryPort.save(session);

        return assistantMessage;
    }

    /**
     * Sends a message in the default session.
     *
     * @param userMessage user message
     * @return AI response message
     */
    public ChatMessage executeInDefaultSession(String userMessage) {
        ChatSession session = repositoryPort.getOrCreateDefaultSession();

        session.addUserMessage(userMessage);

        String aiResponse = aiChatPort.chat(userMessage);

        ChatMessage assistantMessage = session.addAssistantMessage(aiResponse);

        repositoryPort.save(session);

        return assistantMessage;
    }
}
