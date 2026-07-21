package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.RoutingResult;
import com.ai.workflow.domain.service.RoutingWorkflow;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Classification + specialized prompt aligned with Spring AI agentic-patterns/routing-workflow.
 */
@Component
public class SpringAiRoutingWorkflow implements RoutingWorkflow {

    private final ChatClientProvider chatClientProvider;

    public SpringAiRoutingWorkflow(ChatClientProvider chatClientProvider) {
        this.chatClientProvider = Objects.requireNonNull(chatClientProvider);
    }

    @Override
    public RoutingResult route(String input, Map<String, String> routes) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(routes, "routes");
        if (routes.isEmpty()) {
            throw new IllegalArgumentException("routes must not be empty");
        }

        RouteClassification classification = classify(input, routes.keySet());
        String selection = classification.selection();
        String selectedPrompt = routes.get(selection);
        if (selectedPrompt == null) {
            throw new IllegalArgumentException("Selected route '" + selection + "' not found in routes map");
        }

        String output = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .user(selectedPrompt + "\nInput: " + input)
                .call()
                .content();

        return new RoutingResult(
                selection,
                classification.reasoning(),
                output == null ? "" : output);
    }

    private RouteClassification classify(String input, Set<String> availableRoutes) {
        String selectorPrompt = """
                Analyze the input and select the most appropriate support team from these options: %s
                First explain your reasoning, then provide your selection in this JSON format:

                {
                    "reasoning": "Brief explanation of why this ticket should be routed to a specific team. Consider key terms, user intent, and urgency level.",
                    "selection": "The chosen team name"
                }

                Input: %s""".formatted(availableRoutes, input);

        RouteClassification classification = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .user(selectorPrompt)
                .call()
                .entity(RouteClassification.class);

        if (classification == null || classification.selection() == null || classification.selection().isBlank()) {
            throw new IllegalStateException("Routing classification returned empty selection");
        }
        return classification;
    }

    record RouteClassification(String reasoning, String selection) {
    }
}
