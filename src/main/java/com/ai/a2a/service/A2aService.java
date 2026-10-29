package com.ai.a2a.service;

import com.ai.a2a.domain.model.A2aMessage;
import com.ai.a2a.domain.model.A2aSkill;
import com.ai.a2a.domain.model.A2aTask;
import com.ai.a2a.domain.model.A2aTaskStatus;
import com.ai.a2a.domain.model.AgentCard;
import com.ai.agent.domain.model.AgentRun;
import com.ai.agent.service.AgentService;
import com.ai.common.exception.DomainException;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.repository.AgentRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Minimal A2A Server: Agent Card discovery and Task messaging over the Agent façade. */
@Service
@RequiredArgsConstructor
public class A2aService {

  private final AgentService agentService;
  private final AgentRegistry agentRegistry;

  /** Builds the public Agent Card from builtin worker Agent types (no owner data). */
  public AgentCard buildAgentCard(String baseUrl) {
    List<A2aSkill> skills =
        agentRegistry.listBuiltins("en").stream()
            .filter(AgentDefinition::isWorker)
            .map(
                def ->
                    A2aSkill.createSkill(
                        def.getType().getValue(), def.getName(), def.getDescription()))
            .toList();
    return AgentCard.createCard(baseUrl, skills);
  }

  /** Runs message:send → one completed or failed Task. */
  public A2aTask sendMessage(A2aMessage message, String ownerKey, String language) {
    String taskId = UUID.randomUUID().toString();
    A2aTask task = A2aTask.createSubmitted(taskId);
    task.markWorking();
    try {
      String agentType = message.resolveAgentType(AgentService.DEFAULT_AGENT_TYPE);
      AgentRun run = agentService.invoke(agentType, message.extractText(), ownerKey, language);
      task.completeWithText(run.getReply());
    } catch (RuntimeException e) {
      task.failWithMessage(safeFailureMessage(e));
    }
    return task;
  }

  /** Streams Task lifecycle events for message:stream. */
  public Flux<ServerSentEvent<Map<String, Object>>> streamMessage(
      A2aMessage message, String ownerKey, String language) {
    String taskId = UUID.randomUUID().toString();
    A2aTask submitted = A2aTask.createSubmitted(taskId);
    A2aTask working = A2aTask.createSubmitted(taskId);
    working.markWorking();

    return Flux.concat(
        Flux.just(event("task", submitted.toWireMap())),
        Flux.just(event("status-update", working.toWireMap())),
        Mono.fromCallable(() -> runToTerminalTask(taskId, message, ownerKey, language))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(
                task -> {
                  if (task.getStatus() == A2aTaskStatus.COMPLETED) {
                    return Flux.just(
                        event("artifact-update", task.toWireMap()),
                        event("status-update", task.toWireMap()));
                  }
                  return Flux.just(event("status-update", task.toWireMap()));
                }));
  }

  private A2aTask runToTerminalTask(
      String taskId, A2aMessage message, String ownerKey, String language) {
    A2aTask task = A2aTask.createSubmitted(taskId);
    task.markWorking();
    try {
      String agentType = message.resolveAgentType(AgentService.DEFAULT_AGENT_TYPE);
      AgentRun run = agentService.invoke(agentType, message.extractText(), ownerKey, language);
      task.completeWithText(run.getReply());
    } catch (RuntimeException e) {
      task.failWithMessage(safeFailureMessage(e));
    }
    return task;
  }

  /** Avoid echoing provider/runtime text that may include prompt fragments. */
  static String safeFailureMessage(RuntimeException error) {
    if (error instanceof DomainException domain) {
      return domain.getCode();
    }
    return "agent_invoke_failed";
  }

  private static ServerSentEvent<Map<String, Object>> event(String name, Map<String, Object> data) {
    return ServerSentEvent.<Map<String, Object>>builder().event(name).data(data).build();
  }
}
