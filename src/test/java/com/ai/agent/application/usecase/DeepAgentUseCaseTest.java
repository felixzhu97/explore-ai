package com.ai.agent.application.usecase;

import com.ai.agent.application.AgentEvaluator;
import com.ai.agent.application.AgentPlanner;
import com.ai.agent.domain.model.AgentEvaluation;
import com.ai.agent.domain.model.AgentPlan;
import com.ai.agent.domain.model.AgentSession;
import com.ai.agent.infrastructure.config.DeepAgentProperties;
import com.ai.agent.infrastructure.persistence.InMemoryAgentSessionRepository;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.infrastructure.skills.AgentSkillsRuntime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeepAgentUseCaseTest {

    @Mock
    private AgentPlanner planner;
    @Mock
    private AgentEvaluator evaluator;
    @Mock
    private ChatClientProvider chatClients;
    @Mock
    private ChatMemory chatMemory;
    @Mock
    private AgentSkillsRuntime skillsRuntime;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.StreamResponseSpec streamSpec;

    private DeepAgentUseCase useCase;

    @BeforeEach
    void setUp() {
        DeepAgentProperties props = new DeepAgentProperties();
        props.setMaxIterations(3);
        org.mockito.Mockito.lenient()
                .when(skillsRuntime.augmentSystemPrompt(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        useCase = new DeepAgentUseCase(
                new InMemoryAgentSessionRepository(),
                planner,
                evaluator,
                chatClients,
                chatMemory,
                skillsRuntime,
                props,
                new ObjectMapper());
    }

    @Test
    void should_createAndListSessions_when_clientOwnsThem() {
        AgentSession created = useCase.createSession("Research", "c1");
        assertThat(useCase.listSessions("c1")).extracting(AgentSession::title).containsExactly("Research");
        assertThat(useCase.listSessions("other")).isEmpty();
        assertThat(useCase.getSession(created.id().value(), "c1").title()).isEqualTo("Research");
    }

    @Test
    void should_emitPlanMessageDone_when_evaluationPasses() {
        AgentPlan plan = new AgentPlan("sum", "goal", List.of("step"), "thinking");
        when(planner.plan(anyString(), anyString())).thenReturn(plan);
        when(evaluator.evaluate(anyString(), any(), anyString()))
                .thenReturn(new AgentEvaluation(AgentEvaluation.Verdict.PASS, "ok"));
        stubStreamingAnswer("hello");

        AgentSession session = useCase.createSession("t", "c1");

        StepVerifier.create(useCase.invoke(session.id().value(), "do work", "c1"))
                .assertNext(e -> assertThat(e.event()).isEqualTo("plan"))
                .assertNext(e -> assertThat(e.event()).isEqualTo("thought"))
                .assertNext(e -> {
                    assertThat(e.event()).isEqualTo("message");
                    assertThat(e.data()).isEqualTo("hello");
                })
                .assertNext(e -> assertThat(e.event()).isEqualTo("evaluation"))
                .assertNext(e -> assertThat(e.event()).isEqualTo("done"))
                .verifyComplete();
    }

    @Test
    void should_replan_when_needsImprovementThenPass() {
        AgentPlan plan = new AgentPlan("sum", "goal", List.of("step"), "");
        when(planner.plan(anyString(), anyString())).thenReturn(plan);
        when(evaluator.evaluate(anyString(), any(), anyString()))
                .thenReturn(new AgentEvaluation(AgentEvaluation.Verdict.NEEDS_IMPROVEMENT, "more detail"))
                .thenReturn(new AgentEvaluation(AgentEvaluation.Verdict.PASS, "ok"));
        stubStreamingAnswer("answer");

        AgentSession session = useCase.createSession("t", "c1");

        StepVerifier.create(useCase.invoke(session.id().value(), "task", "c1"))
                .recordWith(java.util.ArrayList::new)
                .thenConsumeWhile(e -> true)
                .consumeRecordedWith(events -> {
                    List<String> types = events.stream().map(ServerSentEvent::event).toList();
                    assertThat(types).contains("plan", "replan", "evaluation", "done");
                    assertThat(types.stream().filter("replan"::equals).count()).isEqualTo(1);
                })
                .verifyComplete();
    }

    private void stubStreamingAnswer(String answer) {
        when(chatClients.create(any(), any(), anyString())).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(org.mockito.ArgumentMatchers.<java.util.function.Consumer>any()))
                .thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just(answer));
    }
}
