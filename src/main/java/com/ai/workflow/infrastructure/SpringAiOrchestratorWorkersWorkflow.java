package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.OrchestratorWorkersResult;
import com.ai.workflow.domain.model.WorkerTask;
import com.ai.workflow.domain.service.OrchestratorWorkersWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrator-workers aligned with Spring AI agentic-patterns/orchestrator-workers,
 * with parallel workers via CompletableFuture and a synthesizer step.
 */
@Component
public class SpringAiOrchestratorWorkersWorkflow implements OrchestratorWorkersWorkflow {

    static final String DEFAULT_ORCHESTRATOR_PROMPT = """
            Analyze this task and break it down into 2-3 distinct approaches:

            Task: {task}

            Return your response in this JSON format:
            {
            "analysis": "Explain your understanding of the task and which variations would be valuable. Focus on how each approach serves different aspects of the task.",
            "tasks": [
                {
                "type": "formal",
                "description": "Write a precise, technical version that emphasizes specifications"
                },
                {
                "type": "conversational",
                "description": "Write an engaging, friendly version that connects with readers"
                }
            ]
            }
            """;

    static final String DEFAULT_WORKER_PROMPT = """
            Generate content based on:
            Task: {original_task}
            Style: {task_type}
            Guidelines: {task_description}
            """;

    static final String DEFAULT_SYNTHESIZER_PROMPT = """
            Combine the worker results into one cohesive final answer for the original task.
            Preserve distinctive useful details from each worker without repeating yourself.

            Original task:
            {task}

            Worker results:
            {worker_results}
            """;

    private final ChatClientProvider chatClientProvider;
    private final String orchestratorPrompt;
    private final String workerPrompt;
    private final String synthesizerPrompt;

    @org.springframework.beans.factory.annotation.Autowired
    public SpringAiOrchestratorWorkersWorkflow(ChatClientProvider chatClientProvider) {
        this(chatClientProvider, DEFAULT_ORCHESTRATOR_PROMPT, DEFAULT_WORKER_PROMPT, DEFAULT_SYNTHESIZER_PROMPT);
    }

    private SpringAiOrchestratorWorkersWorkflow(
            ChatClientProvider chatClientProvider,
            String orchestratorPrompt,
            String workerPrompt,
            String synthesizerPrompt) {
        this.chatClientProvider = Objects.requireNonNull(chatClientProvider);
        this.orchestratorPrompt = Objects.requireNonNull(orchestratorPrompt);
        this.workerPrompt = Objects.requireNonNull(workerPrompt);
        this.synthesizerPrompt = Objects.requireNonNull(synthesizerPrompt);
    }

    @Override
    public OrchestratorWorkersResult process(String task) {
        if (task == null || task.isBlank()) {
            throw new IllegalArgumentException("task must not be empty");
        }

        OrchestratorPlan plan = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .user(orchestratorPrompt.replace("{task}", task))
                .call()
                .entity(OrchestratorPlan.class);

        if (plan == null || plan.tasks() == null || plan.tasks().isEmpty()) {
            throw new IllegalStateException("Orchestrator returned no tasks");
        }

        List<WorkerTask> tasks = plan.tasks().stream()
                .map(t -> new WorkerTask(t.type(), t.description()))
                .toList();

        List<CompletableFuture<String>> futures = plan.tasks().stream()
                .map(workerTask -> CompletableFuture.supplyAsync(() -> invokeWorker(task, workerTask)))
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        List<String> workerResponses = futures.stream().map(CompletableFuture::join).toList();

        String joined = String.join("\n---\n", workerResponses);
        String synthesis = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .user(synthesizerPrompt
                        .replace("{task}", task)
                        .replace("{worker_results}", joined))
                .call()
                .content();

        return new OrchestratorWorkersResult(
                plan.analysis(),
                tasks,
                workerResponses,
                synthesis == null ? "" : synthesis);
    }

    private String invokeWorker(String originalTask, PlannedTask workerTask) {
        String content = chatClientProvider
                .createBareStateless(TextChatOptions.defaults())
                .prompt()
                .user(workerPrompt
                        .replace("{original_task}", originalTask)
                        .replace("{task_type}", workerTask.type())
                        .replace("{task_description}", workerTask.description()))
                .call()
                .content();
        return content == null ? "" : content;
    }

    record PlannedTask(String type, String description) {
    }

    record OrchestratorPlan(String analysis, List<PlannedTask> tasks) {
    }
}
