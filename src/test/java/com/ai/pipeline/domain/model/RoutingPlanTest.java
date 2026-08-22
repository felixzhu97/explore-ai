package com.ai.pipeline.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ai.pipeline.domain.vo.AgentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RoutingPlan")
class RoutingPlanTest {

  @Test
  void shouldCreateSingleWorkerPlan() {
    RoutingPlan plan = RoutingPlan.single(AgentType.of("k8s"), "pods");
    assertEquals("k8s", plan.primaryAgent().value());
    assertTrue(plan.subtasks().isEmpty());
  }

  @Test
  void shouldCopyNullSubtasksAsEmpty() {
    RoutingPlan plan = new RoutingPlan(AgentType.of("aiops"), "reason", null);
    assertTrue(plan.subtasks().isEmpty());
  }

  @Test
  void shouldRejectSupervisorAsPrimary() {
    assertThrows(
        IllegalArgumentException.class, () -> RoutingPlan.single(AgentType.supervisor(), "bad"));
  }

  @Test
  void shouldRejectSupervisorSubtask() {
    assertThrows(
        IllegalArgumentException.class, () -> new RoutingPlan.Subtask(AgentType.supervisor(), "x"));
  }

  @Test
  void shouldKeepSubtasks() {
    RoutingPlan plan =
        new RoutingPlan(
            AgentType.of("k8s"),
            "multi",
            List.of(new RoutingPlan.Subtask(AgentType.of("aiops"), "check")));
    assertEquals(1, plan.subtasks().size());
  }
}
