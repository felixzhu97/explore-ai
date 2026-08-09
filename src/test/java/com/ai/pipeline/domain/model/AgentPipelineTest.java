package com.ai.pipeline.domain.model;

import com.ai.pipeline.domain.vo.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPipelineTest {

    @Test
    void shouldOrderSingleNodeWithoutEdges() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(AgentPipeline.PipelineNode.of("n1", AgentType.of("k8s"))),
                List.of());

        assertEquals(
                List.of(AgentType.of("k8s")),
                pipeline.executionOrder().stream().map(AgentPipeline.PipelineNode::agentType).toList());
    }

    @Test
    void shouldTopoSortConnectedWorkers() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(
                        AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                        AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
                List.of(new AgentPipeline.PipelineEdge("a", "b")));

        assertEquals(
                List.of(AgentType.of("k8s"), AgentType.of("aiops")),
                pipeline.executionOrder().stream().map(AgentPipeline.PipelineNode::agentType).toList());
    }

    @Test
    void shouldRejectEmptyPipeline() {
        AgentPipeline pipeline = AgentPipeline.create(List.of(), List.of());
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, pipeline::executionOrder);
        assertTrue(error.getMessage().contains("at least one"));
    }

    @Test
    void shouldRejectUnconnectedNodes() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(
                        AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                        AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
                List.of());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, pipeline::executionOrder);
        assertTrue(error.getMessage().contains("connect"));
    }

    @Test
    void shouldRejectCycle() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(
                        AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                        AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
                List.of(
                        new AgentPipeline.PipelineEdge("a", "b"),
                        new AgentPipeline.PipelineEdge("b", "a")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, pipeline::executionOrder);
        assertTrue(error.getMessage().contains("cycle"));
    }

    @Test
    void shouldRejectSupervisorNode() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(AgentPipeline.PipelineNode.of("s", AgentType.supervisor())),
                List.of());

        assertThrows(IllegalArgumentException.class, pipeline::executionOrder);
    }

    @Test
    void shouldKeepNodeSnapshotFields() {
        AgentPipeline.PipelineNode node = new AgentPipeline.PipelineNode(
                "n1",
                AgentType.of("research"),
                "Custom Research",
                "custom desc",
                "Custom prompt",
                List.of("web_search"));

        assertEquals("Custom Research", node.name());
        assertEquals("Custom prompt", node.systemPrompt());
        assertEquals(List.of("web_search"), node.toolKeys());
        assertEquals("Custom prompt", node.toDefinition().systemPrompt());
    }
}
