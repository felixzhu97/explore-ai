package com.ai.agent.application.usecase;

import com.ai.agent.application.port.SupervisorRouter;
import com.ai.agent.application.port.WorkerAgentInvoker;
import com.ai.agent.domain.exception.AgentNotFoundException;
import com.ai.agent.domain.model.AgentDefinition;
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

/**
 * Orchestrator-Workers workflow: route via supervisor, run workers, emit SSE events.
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
                    StringBuilder collected = new StringBuilder();
                    for (RoutingPlan.Subtask subtask : plan.subtasks()) {
                        AgentDefinition worker = registry.require(subtask.agentType());
                        String result = workerInvoker.invoke(worker, subtask.instruction());
                        collected.append("### ").append(subtask.agentType().value()).append('\n')
                                .append(result).append("\n\n");
                    }
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
