package com.ai.pipeline.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RoutingPlan")
class RoutingPlanTest {

  @Test
  void shouldCreateSingleWorkerPlan() {
    RoutingPlan plan = RoutingPlan.createSingleAgentPlan(AgentType.createType("k8s"), "pods");
    assertEquals("k8s", plan.primaryAgent().value());
    assertTrue(plan.subtasks().isEmpty());
  }

  @Test
  void shouldCopyNullSubtasksAsEmpty() {
    RoutingPlan plan = new RoutingPlan(AgentType.createType("aiops"), "reason", null);
    assertTrue(plan.subtasks().isEmpty());
  }

  @Test
  void shouldRejectSupervisorAsPrimary() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RoutingPlan.createSingleAgentPlan(AgentType.createSupervisorType(), "bad"));
  }

  @Test
  void shouldRejectSupervisorSubtask() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RoutingPlan.Subtask(AgentType.createSupervisorType(), "x"));
  }

  @Test
  void shouldKeepSubtasks() {
    RoutingPlan plan =
        new RoutingPlan(
            AgentType.createType("k8s"),
            "multi",
            List.of(new RoutingPlan.Subtask(AgentType.createType("aiops"), "check")));
    assertEquals(1, plan.subtasks().size());
  }
}
