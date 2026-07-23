package com.ai.agent.application.usecase;

import com.ai.agent.application.port.SupervisorRouter;
import com.ai.agent.application.port.WorkerAgentInvoker;
import com.ai.agent.domain.exception.AgentNotFoundException;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.model.AgentPipeline;
import com.ai.agent.domain.model.RoutingPlan;
import com.ai.agent.domain.repository.AgentRegistry;
import com.ai.agent.domain.vo.AgentType;
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

/**
 * Orchestrator-Workers workflow: route via supervisor, run workers (parallel when multi-subtask),
 * compose pipeline steps by feeding prior output forward.
 */
@Service
public class OrchestratorWorkersUseCase {

    private final AgentRegistry registry;
    private final SupervisorRouter supervisorRouter;
    private final WorkerAgentInvoker workerInvoker;

    public OrchestratorWorkersUseCase(
            AgentRegistry registry,
            SupervisorRouter supervisorRouter,
            WorkerAgentInvoker workerInvoker) {
        this.registry = registry;
        this.supervisorRouter = supervisorRouter;
        this.workerInvoker = workerInvoker;
    }

    public List<AgentDefinition> listAgents() {
        return registry.listAll();
    }

    public AgentDefinition health(AgentType type) {
        return registry.require(type);
    }

    public Flux<ServerSentEvent<String>> invokeSupervisor(String message) {
        if (message == null || message.isBlank()) {
            return Flux.just(errorEvent("message must not be blank"), doneEvent());
        }

        return Mono.fromCallable(() -> {
                    List<AgentDefinition> workers = registry.listWorkers();
                    return supervisorRouter.plan(message, workers);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(plan -> executePlan(message, plan))
                .onErrorResume(err -> Flux.just(
                        errorEvent(err.getMessage() != null ? err.getMessage() : "orchestration failed"),
                        doneEvent()));
    }

    public Flux<ServerSentEvent<String>> invokeAgent(AgentType type, String message) {
        if (message == null || message.isBlank()) {
            return Flux.just(errorEvent("message must not be blank"), doneEvent());
        }
        if (type.isSupervisor()) {
            return invokeSupervisor(message);
        }

        try {
            AgentDefinition agent = registry.require(type);
            return Flux.concat(
                    Flux.just(handoffEvent(type.value(), "direct invoke")),
                    workerInvoker.invokeStream(agent, message)
                            .map(OrchestratorWorkersUseCase::messageEvent),
                    Flux.just(doneEvent()));
        } catch (AgentNotFoundException e) {
            return Flux.just(errorEvent(e.getMessage()), doneEvent());
        }
    }

    public Flux<ServerSentEvent<String>> invokePipeline(String message, AgentPipeline pipeline) {
        if (message == null || message.isBlank()) {
            return Flux.just(errorEvent("message must not be blank"), doneEvent());
        }
        try {
            List<AgentType> order = pipeline.executionOrder();
            return runPipelineStreamed(message, order)
                    .onErrorResume(err -> Flux.just(
                            errorEvent(err.getMessage() != null ? err.getMessage() : "pipeline failed"),
                            doneEvent()));
        } catch (IllegalArgumentException | AgentNotFoundException e) {
            return Flux.just(errorEvent(e.getMessage()), doneEvent());
        }
    }

    /**
     * Emits handoff + streamed worker output per step so clients see stages as they run.
     * Step order and prior-output context forwarding are unchanged.
     */
    private Flux<ServerSentEvent<String>> runPipelineStreamed(String message, List<AgentType> order) {
        AtomicReference<String> current = new AtomicReference<>(message);
        return Flux.fromIterable(order)
                .concatMap(type -> {
                    AgentDefinition agent = registry.require(type);
                    String stepInput = """
                            Original user request:
                            %s

                            Context from previous pipeline step:
                            %s

                            Continue the task as your specialist role.
                            """.formatted(message, current.get());
                    StringBuilder stepOutput = new StringBuilder();
                    return Flux.concat(
                            Flux.just(handoffEvent(type.value(), "pipeline step")),
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

    private Flux<ServerSentEvent<String>> executePlan(String originalMessage, RoutingPlan plan) {
        List<Flux<ServerSentEvent<String>>> stages = new ArrayList<>();
        stages.add(Flux.just(handoffEvent(plan.primaryAgent().value(), plan.reason())));

        if (plan.subtasks().isEmpty()) {
            AgentDefinition primary = registry.require(plan.primaryAgent());
            stages.add(workerInvoker.invokeStream(primary, originalMessage)
                    .map(OrchestratorWorkersUseCase::messageEvent));
        } else {
            stages.add(runSubtasksAndSynthesize(originalMessage, plan));
        }

        stages.add(Flux.just(doneEvent()));
        return Flux.concat(stages);
    }

    private Flux<ServerSentEvent<String>> runSubtasksAndSynthesize(String originalMessage, RoutingPlan plan) {
        return Mono.fromCallable(() -> {
                    List<RoutingPlan.Subtask> subtasks = plan.subtasks();
                    List<String> workerOutputs;
                    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                        List<CompletableFuture<String>> futures = subtasks.stream()
                                .map(subtask -> CompletableFuture.supplyAsync(() -> {
                                    AgentDefinition worker = registry.require(subtask.agentType());
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
                    AgentDefinition synthesizer = registry.require(plan.primaryAgent());
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
