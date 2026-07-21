package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.EvaluatorOptimizerResult;
import com.ai.workflow.domain.model.GenerationStep;
import com.ai.workflow.domain.service.EvaluatorOptimizerWorkflow;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Evaluator-optimizer loop aligned with Spring AI agentic-patterns/evaluator-optimizer,
 * with a max-iteration guard.
 */
@Component
public class SpringAiEvaluatorOptimizerWorkflow implements EvaluatorOptimizerWorkflow {

    static final int DEFAULT_MAX_ITERATIONS = 5;

    static final String DEFAULT_GENERATOR_PROMPT = """
            Your goal is to complete the task based on the input. If there are feedback
            from your previous generations, you should reflect on them to improve your solution.

            Return JSON with fields thoughts and response.
            """;

    static final String DEFAULT_EVALUATOR_PROMPT = """
            Evaluate this implementation for correctness and best practices.
            Respond with JSON fields evaluation and feedback.
            The evaluation field must be one of: PASS, NEEDS_IMPROVEMENT, FAIL.
            Use PASS only if all criteria are met with no improvements needed.
            """;

    private final ChatClientProvider chatClientProvider;
    private final String generatorPrompt;
    private final String evaluatorPrompt;
    private final int maxIterations;

    public SpringAiEvaluatorOptimizerWorkflow(ChatClientProvider chatClientProvider) {
        this(chatClientProvider, DEFAULT_GENERATOR_PROMPT, DEFAULT_EVALUATOR_PROMPT, DEFAULT_MAX_ITERATIONS);
    }

    SpringAiEvaluatorOptimizerWorkflow(
            ChatClientProvider chatClientProvider,
            String generatorPrompt,
            String evaluatorPrompt,
            int maxIterations) {
        this.chatClientProvider = Objects.requireNonNull(chatClientProvider);
        this.generatorPrompt = Objects.requireNonNull(generatorPrompt);
        this.evaluatorPrompt = Objects.requireNonNull(evaluatorPrompt);
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be greater than 0");
        }
        this.maxIterations = maxIterations;
    }

    @Override
    public EvaluatorOptimizerResult loop(String task) {
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("task must not be empty");
        }

        List<String> memory = new ArrayList<>();
        List<GenerationStep> chainOfThought = new ArrayList<>();
        String context = "";

        for (int i = 0; i < maxIterations; i++) {
            GenerationEntity generation = generate(task, context);
            GenerationStep step = new GenerationStep(generation.thoughts(), generation.response());
            chainOfThought.add(step);
            memory.add(generation.response());

            EvaluationEntity evaluation = evaluate(generation.response(), task);
            if (evaluation.evaluation() == EvaluationStatus.PASS) {
                return new EvaluatorOptimizerResult(generation.response(), chainOfThought);
            }

            StringBuilder nextContext = new StringBuilder("Previous attempts:");
            for (String attempt : memory) {
                nextContext.append("\n- ").append(attempt);
            }
            nextContext.append("\nFeedback: ").append(evaluation.feedback());
            context = nextContext.toString();
        }

        GenerationStep last = chainOfThought.getLast();
        return new EvaluatorOptimizerResult(last.response(), chainOfThought);
    }

    private GenerationEntity generate(String task, String context) {
        String userMessage = "%s\n%s\nTask: %s".formatted(generatorPrompt, context, task);
        GenerationEntity entity = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(userMessage)
                .call()
                .entity(GenerationEntity.class);
        if (entity == null || entity.response() == null) {
            throw new IllegalStateException("Generator returned empty response");
        }
        return entity;
    }

    private EvaluationEntity evaluate(String content, String task) {
        String userMessage = "%s\nOriginal task: %s\nContent to evaluate: %s"
                .formatted(evaluatorPrompt, task, content);
        EvaluationEntity entity = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .user(userMessage)
                .call()
                .entity(EvaluationEntity.class);
        if (entity == null || entity.evaluation() == null) {
            throw new IllegalStateException("Evaluator returned empty evaluation");
        }
        return entity;
    }

    enum EvaluationStatus {
        PASS, NEEDS_IMPROVEMENT, FAIL
    }

    record GenerationEntity(String thoughts, String response) {
    }

    record EvaluationEntity(EvaluationStatus evaluation, String feedback) {
    }
}
