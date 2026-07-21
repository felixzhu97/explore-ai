package com.ai.agent.infrastructure.llm;

import com.ai.agent.application.port.SupervisorRouter;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.model.RoutingPlan;
import com.ai.agent.domain.vo.AgentType;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SpringAiSupervisorRouter implements SupervisorRouter {

    private final ChatClientProvider chatClientProvider;

    public SpringAiSupervisorRouter(ChatClientProvider chatClientProvider) {
        this.chatClientProvider = chatClientProvider;
    }

    @Override
    public RoutingPlan plan(String userMessage, List<AgentDefinition> workers) {
        if (workers == null || workers.isEmpty()) {
            throw new IllegalStateException("no worker agents registered");
        }

        Set<String> allowed = workers.stream()
                .map(w -> w.type().value())
                .collect(Collectors.toUnmodifiableSet());

        String catalog = workers.stream()
                .map(w -> "- " + w.type().value() + ": " + w.description())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Analyze the user request and choose worker agent(s).

                Available workers:
                %s

                User request:
                %s

                Return primaryAgent as one of the worker type ids above.
                Only add subtasks when the request clearly needs multiple specialists.
                """.formatted(catalog, userMessage);

        RoutingDecisionResponse decision = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .system("""
                        You are a multi-agent supervisor. Route work to specialized agents.
                        Never set primaryAgent to supervisor. Prefer a single worker when possible.
                        """)
                .user(prompt)
                .call()
                .entity(RoutingDecisionResponse.class, spec -> spec.validateSchema());

        return toPlan(decision, allowed, workers.getFirst().type());
    }

    private RoutingPlan toPlan(
            RoutingDecisionResponse decision,
            Set<String> allowed,
            AgentType fallback) {
        if (decision == null || decision.primaryAgent() == null || decision.primaryAgent().isBlank()) {
            return RoutingPlan.single(fallback, "fallback to first available worker");
        }

        AgentType primary = normalizeWorker(decision.primaryAgent(), allowed, fallback);
        String reason = decision.reason() == null || decision.reason().isBlank()
                ? "routed by supervisor"
                : decision.reason();

        List<RoutingPlan.Subtask> subtasks = new ArrayList<>();
        if (decision.subtasks() != null) {
            for (SubtaskResponse sub : decision.subtasks()) {
                if (sub == null || sub.agentType() == null || sub.instruction() == null) {
                    continue;
                }
                AgentType type = normalizeWorker(sub.agentType(), allowed, null);
                if (type != null && !sub.instruction().isBlank()) {
                    subtasks.add(new RoutingPlan.Subtask(type, sub.instruction().trim()));
                }
            }
        }

        return new RoutingPlan(primary, reason, subtasks);
    }

    private AgentType normalizeWorker(String raw, Set<String> allowed, AgentType fallback) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if ("supervisor".equals(normalized) || !allowed.contains(normalized)) {
            return fallback;
        }
        return AgentType.of(normalized);
    }

    public record RoutingDecisionResponse(
            String primaryAgent,
            String reason,
            List<SubtaskResponse> subtasks) {
    }

    public record SubtaskResponse(String agentType, String instruction) {
    }
}
