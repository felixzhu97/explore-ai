package com.ai.agent.domain.model;

import com.ai.agent.domain.vo.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutingPlanTest {

    @Test
    void should_create_single_worker_plan() {
        RoutingPlan plan = RoutingPlan.single(AgentType.of("k8s"), "pods");
        assertEquals("k8s", plan.primaryAgent().value());
        assertTrue(plan.subtasks().isEmpty());
    }

    @Test
    void should_copy_null_subtasks_as_empty() {
        RoutingPlan plan = new RoutingPlan(AgentType.of("aiops"), "reason", null);
        assertTrue(plan.subtasks().isEmpty());
    }

    @Test
    void should_reject_supervisor_as_primary() {
        assertThrows(IllegalArgumentException.class,
                () -> RoutingPlan.single(AgentType.supervisor(), "bad"));
    }

    @Test
    void should_reject_supervisor_subtask() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoutingPlan.Subtask(AgentType.supervisor(), "x"));
    }

    @Test
    void should_keep_subtasks() {
        RoutingPlan plan = new RoutingPlan(
                AgentType.of("k8s"),
                "multi",
                List.of(new RoutingPlan.Subtask(AgentType.of("aiops"), "check")));
        assertEquals(1, plan.subtasks().size());
    }
}
