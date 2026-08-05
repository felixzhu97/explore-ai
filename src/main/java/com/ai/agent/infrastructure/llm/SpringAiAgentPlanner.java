package com.ai.agent.infrastructure.llm;

import com.ai.agent.application.AgentPlanner;
import com.ai.agent.domain.model.AgentPlan;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpringAiAgentPlanner implements AgentPlanner {

    private static final String PROMPT = """
            Create a short execution plan for the user task.
            If prior feedback is provided, revise the plan accordingly.
            Return JSON with fields: summary, goal, steps (array of strings), thoughts.
            """;

    private final ChatClientProvider chatClientProvider;

    public SpringAiAgentPlanner(ChatClientProvider chatClientProvider) {
        this.chatClientProvider = chatClientProvider;
    }

    @Override
    public AgentPlan plan(String userMessage, String priorFeedback) {
        StringBuilder user = new StringBuilder(userMessage == null ? "" : userMessage);
        if (priorFeedback != null && !priorFeedback.isBlank()) {
            user.append("\n\nPrior feedback to address:\n").append(priorFeedback);
        }
        PlanEntity entity = chatClientProvider.createBareStateless(TextChatOptions.defaults())
                .prompt()
                .system(PROMPT)
                .user(user.toString())
                .call()
                .entity(PlanEntity.class);
        if (entity == null) {
            return new AgentPlan("Direct answer", userMessage, List.of("Answer the user request"), "");
        }
        return new AgentPlan(
                nullTo(entity.summary(), "Plan"),
                nullTo(entity.goal(), userMessage),
                entity.steps(),
                nullTo(entity.thoughts(), ""));
    }

    private static String nullTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record PlanEntity(String summary, String goal, List<String> steps, String thoughts) {
    }
}
