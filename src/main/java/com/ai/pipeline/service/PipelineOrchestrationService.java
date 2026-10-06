package com.ai.pipeline.service;

import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.pipeline.domain.exception.AgentNotFoundException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.model.RoutingPlan;
import com.ai.pipeline.domain.repository.AgentRegistry;
import com.ai.pipeline.domain.vo.AgentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Orchestrator-workers engine that runs and records supervisor, agent and pipeline calls. */
@Service
@RequiredArgsConstructor
public class PipelineOrchestrationService {

  private final AgentRegistry registry;
  private final SupervisorRouter supervisorRouter;
  private final WorkerAgentInvoker workerInvoker;
  private final AiInvocationRecorder invocationRecorder;

  /** Lists the agents. */
  public List<AgentDefinition> listAgents(String ownerKey, String language) {
    return registry.listAll(ownerKey, language);
  }

  /** Returns an agent with its health. */
  public AgentDefinition getHealth(AgentType type, String ownerKey, String language) {
    return registry.require(type, ownerKey, language);
  }

  /** Streams a supervisor run as SSE: plans routing, runs workers and synthesizes the answer. */
  public Flux<ServerSentEvent<String>> invokeSupervisor(
      String message, String ownerKey, String language) {
    long startedAt = System.nanoTime();
    return Mono.fromCallable(
            () -> {
              List<AgentDefinition> workers = registry.listWorkers(ownerKey, language);
              return supervisorRouter.plan(message, workers);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(plan -> executePlan(message, plan, ownerKey, language))
        .doOnComplete(() -> recordAgent("supervisor", "agent.supervisor", startedAt, true, null))
        .doOnError(
            err ->
                recordAgent("supervisor", "agent.supervisor", startedAt, false, err.getMessage()))
        .onErrorResume(
            err ->
                Flux.just(
                    buildErrorEvent(
                        err.getMessage() != null ? err.getMessage() : "orchestration failed"),
                    buildDoneEvent()));
  }

  /** Streams a direct invocation of one worker agent as SSE, delegating supervisor types. */
  public Flux<ServerSentEvent<String>> invokeAgent(
      AgentType type, String message, String ownerKey, String language) {
    if (type.isSupervisor()) {
      return invokeSupervisor(message, ownerKey, language);
    }

    long startedAt = System.nanoTime();
    try {
      AgentDefinition agent = registry.require(type, ownerKey, language);
      return Flux.concat(
              Flux.just(buildHandoffEvent(type.value(), "direct invoke")),
              workerInvoker
                  .invokeStream(agent, message)
                  .map(PipelineOrchestrationService::buildMessageEvent),
              Flux.just(buildDoneEvent()))
          .doOnComplete(() -> recordAgent(type.value(), "agent.invoke", startedAt, true, null))
          .doOnError(
              err -> recordAgent(type.value(), "agent.invoke", startedAt, false, err.getMessage()));
    } catch (AgentNotFoundException e) {
      recordAgent(type.value(), "agent.invoke", startedAt, false, e.getMessage());
      return Flux.just(buildErrorEvent(e.getMessage()), buildDoneEvent());
    }
  }

  private void recordAgent(
      String agentType, String operation, long startedAt, boolean success, String errorMessage) {
    long latencyMs = (System.nanoTime() - startedAt) / 1_000_000L;
    invocationRecorder.record(
        AiInvocationEvent.builder()
            .domain(AiDomain.AGENTS)
            .operation(operation)
            .outcome(success ? InvocationOutcome.SUCCESS : InvocationOutcome.ERROR)
            .latencyMs(latencyMs)
            .agentType(agentType)
            .errorMessage(errorMessage)
            .build());
  }

  /** Streams a pipeline run as SSE, feeding each node's output into the next node in order. */
  public Flux<ServerSentEvent<String>> invokePipeline(
      String message, AgentPipeline pipeline, String ownerKey, String language) {
    try {
      List<AgentPipeline.PipelineNode> order = pipeline.resolveExecutionOrder();
      return runPipelineStreamed(message, order, ownerKey, language)
          .onErrorResume(
              err ->
                  Flux.just(
                      buildErrorEvent(
                          err.getMessage() != null ? err.getMessage() : "pipeline failed"),
                      buildDoneEvent()));
    } catch (IllegalArgumentException | AgentNotFoundException e) {
      return Flux.just(buildErrorEvent(e.getMessage()), buildDoneEvent());
    }
  }

  /** Blocking pipeline execution for background automation (no SSE). */
  public String invokePipelineSync(
      String message, AgentPipeline pipeline, String ownerKey, String language) {
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank");
    }
    List<AgentPipeline.PipelineNode> order = pipeline.resolveExecutionOrder();
    String current = message;
    StringBuilder all = new StringBuilder();
    long startedAt = System.nanoTime();
    try {
      for (AgentPipeline.PipelineNode node : order) {
        AgentDefinition agent = resolveNode(node, ownerKey, language);
        String stepInput =
            """
                        Original user request:
                        %s

                        Context from previous pipeline step:
                        %s

                        Continue the task as your specialist role.
                        """
                .formatted(message, current);
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
      String message, List<AgentPipeline.PipelineNode> order, String ownerKey, String language) {
    AtomicReference<String> current = new AtomicReference<>(message);
    return Flux.fromIterable(order)
        .concatMap(
            node -> {
              AgentDefinition agent = resolveNode(node, ownerKey, language);
              String stepInput =
                  """
                            Original user request:
                            %s

                            Context from previous pipeline step:
                            %s

                            Continue the task as your specialist role.
                            """
                      .formatted(message, current.get());
              StringBuilder stepOutput = new StringBuilder();
              return Flux.concat(
                  Flux.just(buildHandoffEvent(node.agentType().value(), "pipeline step")),
                  workerInvoker
                      .invokeStream(agent, stepInput)
                      .doOnNext(stepOutput::append)
                      .map(PipelineOrchestrationService::buildMessageEvent),
                  Mono.fromRunnable(() -> current.set(stepOutput.toString()))
                      .thenMany(Flux.just(buildMessageEvent("\n\n"))));
            })
        .concatWith(Flux.just(buildDoneEvent()));
  }

  private Flux<ServerSentEvent<String>> executePlan(
      String originalMessage, RoutingPlan plan, String ownerKey, String language) {
    List<Flux<ServerSentEvent<String>>> stages = new ArrayList<>();
    stages.add(Flux.just(buildHandoffEvent(plan.primaryAgent().value(), plan.reason())));

    if (plan.subtasks().isEmpty()) {
      AgentDefinition primary = registry.require(plan.primaryAgent(), ownerKey, language);
      stages.add(
          workerInvoker
              .invokeStream(primary, originalMessage)
              .map(PipelineOrchestrationService::buildMessageEvent));
    } else {
      stages.add(runSubtasksAndSynthesize(originalMessage, plan, ownerKey, language));
    }

    stages.add(Flux.just(buildDoneEvent()));
    return Flux.concat(stages);
  }

  private Flux<ServerSentEvent<String>> runSubtasksAndSynthesize(
      String originalMessage, RoutingPlan plan, String ownerKey, String language) {
    return Mono.fromCallable(
            () -> {
              List<RoutingPlan.Subtask> subtasks = plan.subtasks();
              List<String> workerOutputs;
              try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                List<CompletableFuture<String>> futures =
                    subtasks.stream()
                        .map(
                            subtask ->
                                CompletableFuture.supplyAsync(
                                    () -> {
                                      AgentDefinition worker =
                                          registry.require(subtask.agentType(), ownerKey, language);
                                      String result =
                                          workerInvoker.invoke(worker, subtask.instruction());
                                      return "### "
                                          + subtask.agentType().value()
                                          + '\n'
                                          + result
                                          + "\n\n";
                                    },
                                    pool))
                        .toList();
                workerOutputs = futures.stream().map(CompletableFuture::join).toList();
              }
              String collected = String.join("", workerOutputs);
              AgentDefinition synthesizer =
                  registry.require(plan.primaryAgent(), ownerKey, language);
              String synthesisPrompt =
                  """
                            Original user request:
                            %s

                            Worker results:
                            %s

                            Produce a single cohesive answer for the user.
                            """
                      .formatted(originalMessage, collected);
              return workerInvoker.invoke(synthesizer, synthesisPrompt);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(
            text ->
                Flux.fromArray(text.split("(?<=\\s)"))
                    .map(PipelineOrchestrationService::buildMessageEvent));
  }

  private AgentDefinition resolveNode(
      AgentPipeline.PipelineNode node, String ownerKey, String language) {
    if (node.systemPrompt() != null && !node.systemPrompt().isBlank()) {
      return node.toDefinition();
    }
    AgentDefinition builtin = registry.require(node.agentType(), ownerKey, language);
    String name = node.name() == null || node.name().isBlank() ? builtin.name() : node.name();
    String description =
        node.description() == null || node.description().isBlank()
            ? builtin.description()
            : node.description();
    List<String> tools =
        node.toolKeys() == null || node.toolKeys().isEmpty() ? builtin.toolKeys() : node.toolKeys();
    return AgentDefinition.create(
        node.agentType(),
        name,
        description,
        builtin.systemPrompt(),
        tools,
        AgentDefinition.RUNTIME_SINGLE);
  }

  /** Builds an SSE message event. */
  static ServerSentEvent<String> buildMessageEvent(String data) {
    return ServerSentEvent.<String>builder().event("message").data(data).build();
  }

  /** Builds an SSE handoff event. */
  static ServerSentEvent<String> buildHandoffEvent(String agentType, String reason) {
    String payload = PipelineHandoffEvent.of(agentType, reason).toJson();
    return ServerSentEvent.<String>builder().event("agent_handoff").data(payload).build();
  }

  /** Builds the SSE done event. */
  static ServerSentEvent<String> buildDoneEvent() {
    return ServerSentEvent.<String>builder().event("done").data("[DONE]").build();
  }

  /** Builds an SSE error event. */
  static ServerSentEvent<String> buildErrorEvent(String message) {
    return ServerSentEvent.<String>builder().event("error").data(message).build();
  }
}
