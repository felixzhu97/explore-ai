package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.ChainResult;
import com.ai.workflow.domain.service.ChainWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Prompt chaining aligned with Spring AI agentic-patterns/chain-workflow.
 */
@Component
public class SpringAiChainWorkflow implements ChainWorkflow {

    static final String[] DEFAULT_SYSTEM_PROMPTS = {
            """
                    Extract only the numerical values and their associated metrics from the text.
                    Format each as'value: metric' on a new line.
                    Example format:
                    92: customer satisfaction
                    45%: revenue growth""",
            """
                    Convert all numerical values to percentages where possible.
                    If not a percentage or points, convert to decimal (e.g., 92 points -> 92%).
                    Keep one number per line.
                    Example format:
                    92%: customer satisfaction
                    45%: revenue growth""",
            """
                    Sort all lines in descending order by numerical value.
                    Keep the format 'value: metric' on each line.
                    Example:
                    92%: customer satisfaction
                    87%: employee satisfaction""",
            """
                    Format the sorted data as a markdown table with columns:
                    | Metric | Value |
                    |:--|--:|
                    | Customer Satisfaction | 92% | """
    };

    private final ChatClientProvider chatClientProvider;

    public SpringAiChainWorkflow(ChatClientProvider chatClientProvider) {
        this.chatClientProvider = Objects.requireNonNull(chatClientProvider);
    }

    @Override
    public ChainResult chain(String userInput, String[] systemPrompts) {
        Objects.requireNonNull(userInput, "userInput");
        String[] prompts = (systemPrompts == null || systemPrompts.length == 0)
                ? DEFAULT_SYSTEM_PROMPTS
                : systemPrompts;

        String response = userInput;
        List<String> steps = new ArrayList<>();
        steps.add(response);

        for (String prompt : prompts) {
            String input = "{%s}\n {%s}".formatted(prompt, response);
            response = chatClientProvider
                    .createBareStateless(TextChatOptions.defaults())
                    .prompt()
                    .user(input)
                    .call()
                    .content();
            if (response == null) {
                response = "";
            }
            steps.add(response);
        }

        return new ChainResult(response, steps);
    }
}
