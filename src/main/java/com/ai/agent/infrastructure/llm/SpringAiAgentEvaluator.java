package com.ai.agent.infrastructure.llm;

import com.ai.agent.application.AgentEvaluator;
import com.ai.agent.domain.model.AgentEvaluation;
import com.ai.agent.domain.model.AgentPlan;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import org.springframework.stereotype.Component;

@Component
public class SpringAiAgentEvaluator implements AgentEvaluator {

    private static final String PROMPT = """
            Evaluate whether the agent response adequately completes the user task given the plan.
            Return JSON with fields: evaluation (PASS | NEEDS_IMPROVEMENT | FAIL), feedback.
            Use PASS only when the response is sufficient for the user.
            """;

    private final ChatClientProvider chatClientProvider;

    public SpringAiAgentEvaluator(ChatClientProvider chatClientProvider) {
        this.chatClientProvider = chatClientProvider;
    }

    @Override
    public AgentEvaluation evaluate(String userMessage, AgentPlan plan, String agentResponse) {
        String user = """
                User task:
                %s

                Plan:
                %s

                Agent response:
                %s
                """.formatted(userMessage, plan.executionPrompt(), agentResponse);
        EvalEntity entity = chatClientProvider.createBareStateless(TextChatOptions.defaults())
                .prompt()
                .system(PROMPT)
                .user(user)
                .call()
                .entity(EvalEntity.class);
        if (entity == null) {
            return new AgentEvaluation(AgentEvaluation.Verdict.PASS, "No evaluator response; accepting answer.");
        }
        return new AgentEvaluation(AgentEvaluation.Verdict.parse(entity.evaluation()), entity.feedback());
    }

    private record EvalEntity(String evaluation, String feedback) {
    }
}
