package com.ai.agent.domain.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTypeTest {

    @Test
    void should_normalize_to_lowercase_when_created() {
        assertEquals("k8s", AgentType.of("K8S").value());
    }

    @Test
    void should_identify_supervisor() {
        assertTrue(AgentType.supervisor().isSupervisor());
        assertFalse(AgentType.of("k8s").isSupervisor());
    }

    @Test
    void should_reject_blank_type() {
        assertThrows(IllegalArgumentException.class, () -> AgentType.of("  "));
        assertThrows(NullPointerException.class, () -> AgentType.of(null));
    }
}
