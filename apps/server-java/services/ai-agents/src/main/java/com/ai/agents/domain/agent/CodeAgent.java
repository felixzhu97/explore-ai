package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Code generation and analysis agent.
 */
@Component
public class CodeAgent extends AbstractAiAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Code Assistant specialized in programming tasks.

            You can help with:
            - Writing code in various languages
            - Debugging and fixing issues
            - Code review and optimization
            - Explaining complex code
            - Generating documentation
            """;

    public CodeAgent() {
        super("code", "Code Agent", "Code generation and analysis", AgentType.CODE);
    }

    @Override
    public boolean canHandle(AgentRequest request) {
        String msg = request.message().toLowerCase();
        return msg.contains("code") || msg.contains("programming") ||
               msg.contains("function") || msg.contains("debug") ||
               msg.contains("refactor") || msg.contains("implement");
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        return """
                Code Request received.

                User request: %s

                Note: Code generation with LLM integration pending.
                """.formatted(request.message());
    }
}
