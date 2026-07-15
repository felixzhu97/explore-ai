package com.ai.agent.infrastructure.registry;

import com.ai.agent.domain.exception.AgentNotFoundException;
import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.vo.AgentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAgentRegistryTest {

    private InMemoryAgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryAgentRegistry(List.of(
                AgentDefinition.create(
                        AgentType.supervisor(), "Supervisor", "coords", "sys"),
                AgentDefinition.create(
                        AgentType.of("k8s"), "K8s", "cluster", "sys-k8s"),
                AgentDefinition.create(
                        AgentType.of("aiops"), "AIOps", "incidents", "sys-aiops")));
    }

    @Test
    void should_list_all_agents_including_supervisor() {
        assertEquals(3, registry.listAll().size());
    }

    @Test
    void should_list_only_workers() {
        assertEquals(2, registry.listWorkers().size());
        assertTrue(registry.listWorkers().stream().allMatch(AgentDefinition::isWorker));
    }

    @Test
    void should_require_registered_agent() {
        assertEquals("k8s", registry.require(AgentType.of("k8s")).type().value());
    }

    @Test
    void should_throw_when_agent_unknown() {
        assertThrows(AgentNotFoundException.class,
                () -> registry.require(AgentType.of("unknown")));
    }
}
