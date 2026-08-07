package com.ai.pipeline.application.usecase;

import com.ai.pipeline.application.SupervisorRouter;
import com.ai.pipeline.application.WorkerAgentInvoker;
import com.ai.pipeline.domain.exception.AgentNotFoundException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.model.RoutingPlan;
import com.ai.pipeline.domain.repository.AgentRegistry;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrchestratorWorkersUseCase {

    private final AgentRegistry registry;
    private final SupervisorRouter supervisorRouter;
    private final WorkerAgentInvoker workerInvoker;
    private final AiInvocationRecorder invocationRecorder;

    public OrchestratorWorkersUseCase(
            AgentRegistry registry,
            SupervisorRouter supervisorRouter,
            WorkerAgentInvoker workerInvoker,
            AiInvocationRecorder invocationRecorder) {
        this.registry = registry;
        this.supervisorRouter = supervisorRouter;
        this.workerInvoker = workerInvoker;
        this.invocationRecorder = invocationRecorder;
    }

    public List<AgentDefinition> listAgents(String clientId, String language) {
        return registry.listAll(clientId, language);
    }

    public AgentDefinition health(AgentType type, String clientId, String language) {
        return registry.require(type, clientId, language);
    }

    public Flux<ServerSentEvent<String>> invokeSupervisor(String message, String clientId, String language) {
        if (message == null || message.isBlank()) {
            return Flux.just(errorEvent("message must not be blank"), doneEvent());
        }

        long startedAt = System.nanoTime();
        return Mono.fromCallable(() -> {
                    List<AgentDefinition> workers = registry.listWorkers(clientId, language);
                    return supervisorRouter.plan(message, workers);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(plan -> executePlan(message, plan, clientId, language))
                .doOnComplete(() -> recordAgent("supervisor", "agent.supervisor", startedAt, true, null))
                .doOnError(err -> recordAgent("supervisor", "agent.supervisor", startedAt, false, err.getMessage()))
                .onErrorResume(err -> Flux.just(
                        errorEvent(err.getMessage() != null ? err.getMessage() : "orchestration failed"),
                        doneEvent()));
    }

    public Flux<ServerSentEvent<String>> invokeAgent(
            AgentType type, String message, String clientId, String language) {
        if (message == null || message.isBlank()) {
            return Flux.just(errorEvent("message must not be blank"), doneEvent());
        }
        if (type.isSupervisor()) {
            return invokeSupervisor(message, clientId, language);
        }

        long startedAt = System.nanoTime();
        try {
            AgentDefinition agent = registry.require(type, clientId, language);
            return Flux.concat(
                    Flux.just(handoffEvent(type.value(), "direct invoke")),
                    workerInvoker.invokeStream(agent, message)
                            .map(OrchestratorWorkersUseCase::messageEvent),
                    Flux.just(doneEvent()))
                    .doOnComplete(() -> recordAgent(type.value(), "agent.invoke", startedAt, true, null))
                    .doOnError(err -> recordAgent(type.value(), "agent.invoke", startedAt, false, err.getMessage()));
        } catch (AgentNotFoundException e) {
            recordAgent(type.value(), "agent.invoke", startedAt, false, e.getMessage());
            return Flux.just(errorEvent(e.getMessage()), doneEvent());
        }
    }

    private void recordAgent(String agentType, String operation, long startedAt, boolean success, String errorMessage) {
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000L;
        invocationRecorder.record(AiInvocationEvent.builder()
                .domain(AiDomain.AGENTS)
                .operation(operation)
                .outcome(success ? InvocationOutcome.SUCCESS : InvocationOutcome.ERROR)
                .latencyMs(latencyMs)
                .agentType(agentType)
                .errorMessage(errorMessage)
                .build());
    }

    public Flux<ServerSentEvent<String>> invokePipeline(
            String message, AgentPipeline pipeline, String clientId, String language) {
        if (message == null || message.isBlank()) {
            return Flux.just(errorEvent("message must not be blank"), doneEvent());
        }
        try {
            List<AgentPipeline.PipelineNode> order = pipeline.executionOrder();
            return runPipelineStreamed(message, order, clientId, language)
                    .onErrorResume(err -> Flux.just(
                            errorEvent(err.getMessage() != null ? err.getMessage() : "pipeline failed"),
                            doneEvent()));
        } catch (IllegalArgumentException | AgentNotFoundException e) {
            return Flux.just(errorEvent(e.getMessage()), doneEvent());
        }
    }

    /**
     * Blocking pipeline execution for background automation (no SSE).
     */
    public String invokePipelineSync(String message, AgentPipeline pipeline, String clientId, String language) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        List<AgentPipeline.PipelineNode> order = pipeline.executionOrder();
        String current = message;
        StringBuilder all = new StringBuilder();
        long startedAt = System.nanoTime();
        try {
            for (AgentPipeline.PipelineNode node : order) {
                AgentDefinition agent = resolveNode(node, clientId, language);
                String stepInput = """
                        Original user request:
                        %s

                        Context from previous pipeline step:
                        %s

                        Continue the task as your specialist role.
                        """.formatted(message, current);
                String stepOutput = workerInvoker.invoke(agent, stepInput);
                current = stepOutput == null ? "" : stepOutput;
                if (!all.isEmpty()) {
                    all.append("\n\n");
                }
                all.append(current);
            }
            recordAgent("pipeline", "agent.pipeline.sync", startedAt, true, null);
            return all.toString().trim();
        } catch (RuntimeException ex) {
            recordAgent("pipeline", "agent.pipeline.sync", startedAt, false, ex.getMessage());
            throw ex;
        }
    }

    private Flux<ServerSentEvent<String>> runPipelineStreamed(
            String message,
            List<AgentPipeline.PipelineNode> order,
            String clientId,
            String language) {
        AtomicReference<String> current = new AtomicReference<>(message);
        return Flux.fromIterable(order)
                .concatMap(node -> {
                    AgentDefinition agent = resolveNode(node, clientId, language);
                    String stepInput = """
                            Original user request:
                            %s

                            Context from previous pipeline step:
                            %s

                            Continue the task as your specialist role.
                            """.formatted(message, current.get());
                    StringBuilder stepOutput = new StringBuilder();
                    return Flux.concat(
                            Flux.just(handoffEvent(node.agentType().value(), "pipeline step")),
                            workerInvoker.invokeStream(agent, stepInput)
                                    .doOnNext(chunk -> {
                                        if (chunk != null) {
                                            stepOutput.append(chunk);
                                        }
                                    })
                                    .map(OrchestratorWorkersUseCase::messageEvent),
                            Mono.fromRunnable(() -> current.set(stepOutput.toString()))
                                    .thenMany(Flux.just(messageEvent("\n\n"))));
                })
                .concatWith(Flux.just(doneEvent()));
    }

    private Flux<ServerSentEvent<String>> executePlan(
            String originalMessage, RoutingPlan plan, String clientId, String language) {
        List<Flux<ServerSentEvent<String>>> stages = new ArrayList<>();
        stages.add(Flux.just(handoffEvent(plan.primaryAgent().value(), plan.reason())));

        if (plan.subtasks().isEmpty()) {
            AgentDefinition primary = registry.require(plan.primaryAgent(), clientId, language);
            stages.add(workerInvoker.invokeStream(primary, originalMessage)
                    .map(OrchestratorWorkersUseCase::messageEvent));
        } else {
            stages.add(runSubtasksAndSynthesize(originalMessage, plan, clientId, language));
        }

        stages.add(Flux.just(doneEvent()));
        return Flux.concat(stages);
    }

    private Flux<ServerSentEvent<String>> runSubtasksAndSynthesize(
            String originalMessage, RoutingPlan plan, String clientId, String language) {
        return Mono.fromCallable(() -> {
                    List<RoutingPlan.Subtask> subtasks = plan.subtasks();
                    List<String> workerOutputs;
                    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                        List<CompletableFuture<String>> futures = subtasks.stream()
                                .map(subtask -> CompletableFuture.supplyAsync(() -> {
                                    AgentDefinition worker =
                                            registry.require(subtask.agentType(), clientId, language);
                                    String result = workerInvoker.invoke(worker, subtask.instruction());
                                    return "### " + subtask.agentType().value() + '\n'
                                            + result + "\n\n";
                                }, pool))
                                .toList();
                        workerOutputs = futures.stream()
                                .map(CompletableFuture::join)
                                .toList();
                    }
                    String collected = String.join("", workerOutputs);
                    AgentDefinition synthesizer = registry.require(plan.primaryAgent(), clientId, language);
                    String synthesisPrompt = """
                            Original user request:
                            %s

                            Worker results:
                            %s

                            Produce a single cohesive answer for the user.
                            """.formatted(originalMessage, collected);
                    return workerInvoker.invoke(synthesizer, synthesisPrompt);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(text -> Flux.fromArray(text.split("(?<=\\s)"))
                        .map(OrchestratorWorkersUseCase::messageEvent));
    }

    private AgentDefinition resolveNode(
            AgentPipeline.PipelineNode node, String clientId, String language) {
        if (node.systemPrompt() != null && !node.systemPrompt().isBlank()) {
            return node.toDefinition();
        }
        AgentDefinition builtin = registry.require(node.agentType(), clientId, language);
        String name = node.name() == null || node.name().isBlank() ? builtin.name() : node.name();
        String description = node.description() == null || node.description().isBlank()
                ? builtin.description()
                : node.description();
        List<String> tools = node.toolKeys() == null || node.toolKeys().isEmpty()
                ? builtin.toolKeys()
                : node.toolKeys();
        return AgentDefinition.create(
                node.agentType(),
                name,
                description,
                builtin.systemPrompt(),
                tools,
                AgentDefinition.RUNTIME_SINGLE);
    }

    static ServerSentEvent<String> messageEvent(String data) {
        return ServerSentEvent.<String>builder().event("message").data(data).build();
    }

    static ServerSentEvent<String> handoffEvent(String agentType, String reason) {
        String payload = "{\"agentType\":\"%s\",\"reason\":%s}".formatted(
                agentType, jsonString(reason));
        return ServerSentEvent.<String>builder().event("agent_handoff").data(payload).build();
    }

    static ServerSentEvent<String> doneEvent() {
        return ServerSentEvent.<String>builder().event("done").data("[DONE]").build();
    }

    static ServerSentEvent<String> errorEvent(String message) {
        return ServerSentEvent.<String>builder().event("error").data(message).build();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
