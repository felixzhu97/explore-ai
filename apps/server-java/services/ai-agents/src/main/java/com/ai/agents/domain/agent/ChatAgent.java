package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * General chat agent for conversational interactions.
 */
@Component
public class ChatAgent extends AbstractAiAgent {

    private static final String SYSTEM_PROMPT = """
            You are a helpful AI assistant. You provide clear, accurate, and helpful responses to user questions.

            Guidelines:
            - Be concise but thorough
            - If you don't know something, say so honestly
            - Ask clarifying questions when needed
            - Provide examples when helpful
            """;

    public ChatAgent() {
        super("chat", "Chat Agent", "General conversational AI assistant", AgentType.CHAT);
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        // Placeholder - in production, this would call an LLM
        return "Chat response: " + request.message() + " (Note: LLM integration pending)";
    }
}
