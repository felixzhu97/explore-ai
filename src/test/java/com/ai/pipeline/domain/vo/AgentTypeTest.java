package com.ai.pipeline.domain.vo;

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
    assertEquals("k8s", AgentType.of("K8S").value());
  }

  @Test
  void shouldIdentifySupervisor() {
    assertTrue(AgentType.supervisor().isSupervisor());
    assertFalse(AgentType.of("k8s").isSupervisor());
  }

  @Test
  void shouldRejectBlankType() {
    assertThrows(IllegalArgumentException.class, () -> AgentType.of("  "));
    assertThrows(NullPointerException.class, () -> AgentType.of(null));
  }
}
