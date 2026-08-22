package com.ai.pipeline.service.usecase;

import com.ai.metrics.domain.repository.AiInvocationEventRepository;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.pipeline.domain.model.AgentDefinition;
import com.ai.pipeline.domain.model.AgentPipeline;
import com.ai.pipeline.domain.model.RoutingPlan;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.infra.registry.CatalogAgentRegistry;
import com.ai.pipeline.service.SupervisorRouter;
import com.ai.pipeline.service.WorkerAgentInvoker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DisplayName("OrchestratorWorkersUseCase")
class OrchestratorWorkersUseCaseTest {

  private OrchestratorWorkersUseCase useCase;
  private RecordingInvoker invoker;

  private static AiInvocationRecorder recorder() {
    return new AiInvocationRecorder(
        new AiInvocationEventRepository() {
          @Override
          public void save(com.ai.metrics.domain.model.AiInvocationEvent event) {}

          @Override
          public PageResult findDrilldown(DrilldownQuery query) {
            return new PageResult(java.util.List.of(), 0);
          }
        },
        new SimpleMeterRegistry());
  }

  @BeforeEach
  void setUp() {
    var registry =
        CatalogAgentRegistry.fixed(
            List.of(
                AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "You are k8s"),
                AgentDefinition.create(AgentType.of("aiops"), "AIOps", "ops", "You are aiops")));
    invoker = new RecordingInvoker();
    SupervisorRouter router =
        (message, workers) -> RoutingPlan.single(AgentType.of("k8s"), "kubernetes intent");
    useCase = new OrchestratorWorkersUseCase(registry, router, invoker, recorder());
  }

  @Test
  void shouldEmitHandoffMessageAndDoneWhenSupervisorRoutes() {
    StepVerifier.create(useCase.invokeSupervisor("list pods in prod", null, "en"))
        .assertNext(
            event -> {
              assertEvent(event, "agent_handoff");
              assert event.data().contains("k8s");
            })
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();

    assert invoker.lastAgentType.equals("k8s");
    assert invoker.lastTask.contains("list pods");
  }

  @Test
  void shouldInvokeWorkerDirectlyWhenAgentTypeGiven() {
    StepVerifier.create(useCase.invokeAgent(AgentType.of("aiops"), "detect anomaly", null, "en"))
        .assertNext(event -> assertEvent(event, "agent_handoff"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();

    assert invoker.lastAgentType.equals("aiops");
  }

  @Test
  void shouldDelegateToSupervisorWhenAgentTypeIsSupervisor() {
    StepVerifier.create(useCase.invokeAgent(AgentType.supervisor(), "scale deployment", null, "en"))
        .assertNext(event -> assertEvent(event, "agent_handoff"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();
  }

  @Test
  void shouldEmitErrorWhenAgentUnknown() {
    StepVerifier.create(useCase.invokeAgent(AgentType.of("missing"), "hello", null, "en"))
        .assertNext(event -> assertEvent(event, "error"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();
  }

  @Test
  void shouldEmitErrorWhenMessageBlank() {
    StepVerifier.create(useCase.invokeSupervisor("  ", null, "en"))
        .assertNext(event -> assertEvent(event, "error"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();
  }

  @Test
  void shouldEmitErrorWhenMessageNull() {
    StepVerifier.create(useCase.invokeSupervisor(null, null, "en"))
        .assertNext(event -> assertEvent(event, "error"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();
  }

  @Test
  void shouldEmitErrorWhenDirectInvokeMessageBlank() {
    StepVerifier.create(useCase.invokeAgent(AgentType.of("k8s"), " ", null, "en"))
        .assertNext(event -> assertEvent(event, "error"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();
  }

  @Test
  void shouldSynthesizeSubtasksWhenPlanHasMultipleWorkers() {
    SupervisorRouter multiRouter =
        (message, workers) ->
            new RoutingPlan(
                AgentType.of("k8s"),
                "needs k8s and aiops",
                List.of(
                    new RoutingPlan.Subtask(AgentType.of("k8s"), "check pods"),
                    new RoutingPlan.Subtask(AgentType.of("aiops"), "check anomalies")));
    useCase =
        new OrchestratorWorkersUseCase(
            CatalogAgentRegistry.fixed(
                List.of(
                    AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                    AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "You are k8s"),
                    AgentDefinition.create(
                        AgentType.of("aiops"), "AIOps", "ops", "You are aiops"))),
            multiRouter,
            invoker,
            recorder());

    StepVerifier.create(useCase.invokeSupervisor("pod crash and anomaly", null, "en"))
        .assertNext(event -> assertEvent(event, "agent_handoff"))
        .thenConsumeWhile(event -> "message".equals(event.event()))
        .expectNextMatches(event -> "done".equals(event.event()))
        .verifyComplete();

    assert invoker.invokeCount.get() >= 3;
  }

  @Test
  void shouldEmitErrorWhenRouterFails() {
    SupervisorRouter failing =
        (message, workers) -> {
          throw new IllegalStateException("router down");
        };
    useCase =
        new OrchestratorWorkersUseCase(
            CatalogAgentRegistry.fixed(
                List.of(
                    AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                    AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "You are k8s"))),
            failing,
            invoker,
            recorder());

    StepVerifier.create(useCase.invokeSupervisor("anything", null, "en"))
        .assertNext(
            event -> {
              assertEvent(event, "error");
              assert event.data().contains("router down");
            })
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();
  }

  @Test
  void shouldEscapeQuotesInHandoffReason() {
    ServerSentEvent<String> event =
        OrchestratorWorkersUseCase.handoffEvent("k8s", "say \"hello\" \\world");
    assertEvent(event, "agent_handoff");
    assert event.data().contains("\\\"hello\\\"");
  }

  @Test
  void shouldUseEmptyReasonWhenNull() {
    ServerSentEvent<String> event = OrchestratorWorkersUseCase.handoffEvent("k8s", null);
    assert event.data().contains("\"reason\":\"\"");
  }

  @Test
  void shouldRunPipelineStepsInOrder() {
    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(
                AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
            List.of(new AgentPipeline.PipelineEdge("a", "b")));

    StepVerifier.create(useCase.invokePipeline("investigate outage", pipeline, null, "en"))
        .assertNext(
            event -> {
              assertEvent(event, "agent_handoff");
              assert event.data().contains("k8s");
            })
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(
            event -> {
              assertEvent(event, "agent_handoff");
              assert event.data().contains("aiops");
            })
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();

    assert invoker.streamOrder.equals(List.of("k8s", "aiops"));
    assert invoker.lastTask.contains("worker-reply");
    assert invoker.lastTask.contains("investigate outage");
  }

  @Test
  void shouldPreferNodeSnapshotPromptWhenPresent() {
    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(
                new AgentPipeline.PipelineNode(
                    "a",
                    AgentType.of("k8s"),
                    "Custom K8s",
                    "custom desc",
                    "You are a custom k8s worker.",
                    List.of("datetime"))),
            List.of());

    StepVerifier.create(useCase.invokePipeline("check pods", pipeline, null, "en"))
        .assertNext(event -> assertEvent(event, "agent_handoff"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();

    assert invoker.lastAgentName.equals("Custom K8s");
    assert invoker.lastSystemPrompt.equals("You are a custom k8s worker.");
    assert invoker.lastToolKeys.equals(List.of("datetime"));
  }

  @Test
  void shouldEmitFirstHandoffBeforeSecondWorkerStarts() {
    DelayedRecordingInvoker delayed = new DelayedRecordingInvoker();
    useCase =
        new OrchestratorWorkersUseCase(
            CatalogAgentRegistry.fixed(
                List.of(
                    AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                    AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "You are k8s"),
                    AgentDefinition.create(
                        AgentType.of("aiops"), "AIOps", "ops", "You are aiops"))),
            (message, workers) -> RoutingPlan.single(AgentType.of("k8s"), "unused"),
            delayed,
            recorder());

    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(
                AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
            List.of(new AgentPipeline.PipelineEdge("a", "b")));

    StepVerifier.create(useCase.invokePipeline("investigate outage", pipeline, null, "en"))
        .assertNext(
            event -> {
              assertEvent(event, "agent_handoff");
              assert event.data().contains("k8s");
              assert !delayed.streamOrder.contains("aiops")
                  : "second worker must not start before first handoff is visible";
            })
        .thenAwait(java.time.Duration.ofMillis(50))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(
            event -> {
              assertEvent(event, "agent_handoff");
              assert event.data().contains("aiops");
            })
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "message"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();

    assert delayed.streamOrder.equals(List.of("k8s", "aiops"));
  }

  @Test
  void shouldEmitErrorWhenPipelineInvalid() {
    AgentPipeline pipeline = AgentPipeline.create(List.of(), List.of());

    StepVerifier.create(useCase.invokePipeline("x", pipeline, null, "en"))
        .assertNext(event -> assertEvent(event, "error"))
        .assertNext(event -> assertEvent(event, "done"))
        .verifyComplete();
  }

  private static void assertEvent(ServerSentEvent<String> event, String expected) {
    assert expected.equals(event.event()) : "expected " + expected + " but was " + event.event();
  }

  private static final class RecordingInvoker implements WorkerAgentInvoker {
    private String lastAgentType;
    private String lastAgentName;
    private String lastSystemPrompt;
    private List<String> lastToolKeys = List.of();
    private String lastTask;
    private final java.util.concurrent.atomic.AtomicInteger invokeCount =
        new java.util.concurrent.atomic.AtomicInteger();
    private final List<String> streamOrder = new java.util.ArrayList<>();

    @Override
    public Flux<String> invokeStream(AgentDefinition agent, String task) {
      lastAgentType = agent.type().value();
      lastAgentName = agent.name();
      lastSystemPrompt = agent.systemPrompt();
      lastToolKeys = agent.toolKeys();
      lastTask = task;
      invokeCount.incrementAndGet();
      streamOrder.add(agent.type().value());
      return Flux.just("worker-reply");
    }

    @Override
    public String invoke(AgentDefinition agent, String task) {
      lastAgentType = agent.type().value();
      lastAgentName = agent.name();
      lastSystemPrompt = agent.systemPrompt();
      lastToolKeys = agent.toolKeys();
      lastTask = task;
      invokeCount.incrementAndGet();
      return "worker-reply for " + agent.type().value();
    }
  }

  private static final class DelayedRecordingInvoker implements WorkerAgentInvoker {
    private final List<String> streamOrder = new java.util.ArrayList<>();

    @Override
    public Flux<String> invokeStream(AgentDefinition agent, String task) {
      streamOrder.add(agent.type().value());
      return Flux.just("worker-reply-" + agent.type().value())
          .delayElements(java.time.Duration.ofMillis(120));
    }

    @Override
    public String invoke(AgentDefinition agent, String task) {
      return invokeStream(agent, task).blockFirst();
    }
  }
}
