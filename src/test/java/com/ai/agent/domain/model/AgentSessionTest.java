package com.ai.agent.domain.model;

import com.ai.agent.domain.vo.SessionId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentSessionTest {

    @Test
    void should_createSession_withGeneratedId_when_titleBlank() {
        AgentSession session = AgentSession.create("  ", "client-1");
        assertThat(session.title()).isEqualTo("New Agent");
        assertThat(session.id().value()).isNotBlank();
        assertThat(session.ownedBy("client-1")).isTrue();
        assertThat(session.id().conversationId()).startsWith("agent:");
    }

    @Test
    void should_rejectBlankClientId() {
        assertThatThrownBy(() -> AgentSession.create("t", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

class AgentPlanTest {

    @Test
    void should_buildExecutionPrompt_when_stepsPresent() {
        AgentPlan plan = new AgentPlan("s", "g", List.of("a", "b"), "think");
        assertThat(plan.executionPrompt()).contains("Goal: g").contains("1. a").contains("2. b");
    }
}

class AgentEvaluationTest {

    @Test
    void should_parseVerdict_when_rawProvided() {
        assertThat(AgentEvaluation.Verdict.parse("pass")).isEqualTo(AgentEvaluation.Verdict.PASS);
        assertThat(AgentEvaluation.Verdict.parse("needs_improvement"))
                .isEqualTo(AgentEvaluation.Verdict.NEEDS_IMPROVEMENT);
        assertThat(new AgentEvaluation(AgentEvaluation.Verdict.PASS, "").passed()).isTrue();
    }

    @Test
    void should_keepSessionId_when_ofCalled() {
        SessionId id = SessionId.of("abc");
        assertThat(id.conversationId()).isEqualTo("agent:abc");
    }
}
