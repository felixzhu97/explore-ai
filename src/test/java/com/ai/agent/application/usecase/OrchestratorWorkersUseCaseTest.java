package com.ai.agent.application.usecase;

import com.ai.agent.application.port.SupervisorRouter;
import com.ai.agent.application.port.WorkerAgentInvoker;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.model.RoutingPlan;
import com.ai.agent.domain.vo.AgentType;
import com.ai.agent.infrastructure.registry.InMemoryAgentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

class OrchestratorWorkersUseCaseTest {

    private OrchestratorWorkersUseCase useCase;
    private RecordingInvoker invoker;

    @BeforeEach
    void setUp() {
        InMemoryAgentRegistry registry = new InMemoryAgentRegistry(List.of(
                AgentDefinition.create(AgentType.supervisor(), "Supervisor", "coords", "sys"),
                AgentDefinition.create(AgentType.of("k8s"), "K8s", "cluster", "You are k8s"),
                AgentDefinition.create(AgentType.of("aiops"), "AIOps", "ops", "You are aiops")));
        invoker = new RecordingInvoker();
        SupervisorRouter router = (message, workers) ->
                RoutingPlan.single(AgentType.of("k8s"), "kubernetes intent");
        useCase = new OrchestratorWorkersUseCase(registry, router, invoker);
    }

    @Test
    void should_emit_handoff_message_and_done_when_supervisor_routes() {
        StepVerifier.create(useCase.invokeSupervisor("list pods in prod"))
                .assertNext(event -> {
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
    void should_invoke_worker_directly_when_agent_type_given() {
        StepVerifier.create(useCase.invokeAgent(AgentType.of("aiops"), "detect anomaly"))
                .assertNext(event -> assertEvent(event, "agent_handoff"))
                .assertNext(event -> assertEvent(event, "message"))
                .assertNext(event -> assertEvent(event, "done"))
                .verifyComplete();

        assert invoker.lastAgentType.equals("aiops");
    }

    @Test
    void should_emit_error_when_agent_unknown() {
        StepVerifier.create(useCase.invokeAgent(AgentType.of("missing"), "hello"))
                .assertNext(event -> assertEvent(event, "error"))
                .assertNext(event -> assertEvent(event, "done"))
                .verifyComplete();
    }

    @Test
    void should_emit_error_when_message_blank() {
        StepVerifier.create(useCase.invokeSupervisor("  "))
                .assertNext(event -> assertEvent(event, "error"))
                .assertNext(event -> assertEvent(event, "done"))
                .verifyComplete();
    }

    private static void assertEvent(ServerSentEvent<String> event, String expected) {
        assert expected.equals(event.event()) : "expected " + expected + " but was " + event.event();
    }

    private static final class RecordingInvoker implements WorkerAgentInvoker {
        private String lastAgentType;
        private String lastTask;

        @Override
        public Flux<String> invokeStream(AgentDefinition agent, String task) {
            lastAgentType = agent.type().value();
            lastTask = task;
            return Flux.just("worker-reply");
        }

        @Override
        public String invoke(AgentDefinition agent, String task) {
            lastAgentType = agent.type().value();
            lastTask = task;
            return "worker-reply";
        }
    }
}
