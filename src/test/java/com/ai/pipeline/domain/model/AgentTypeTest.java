package com.ai.pipeline.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentType")
class AgentTypeTest {

  @Test
  void shouldNormalizeToLowercaseWhenCreated() {
    assertEquals("k8s", AgentType.createType("K8S").value());
  }

  @Test
  void shouldIdentifySupervisor() {
    assertTrue(AgentType.createSupervisorType().isSupervisor());
    assertFalse(AgentType.createType("k8s").isSupervisor());
  }

  @Test
  void shouldRejectBlankType() {
    assertThrows(IllegalArgumentException.class, () -> AgentType.createType("  "));
    assertThrows(NullPointerException.class, () -> AgentType.createType(null));
  }
}
