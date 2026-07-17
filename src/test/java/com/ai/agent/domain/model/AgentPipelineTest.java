package com.ai.agent.domain.model;

import com.ai.agent.domain.vo.AgentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPipelineTest {

    @Test
    void should_order_single_node_without_edges() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(new AgentPipeline.PipelineNode("n1", AgentType.of("k8s"))),
                List.of());

        assertEquals(List.of(AgentType.of("k8s")), pipeline.executionOrder());
    }

    @Test
    void should_topo_sort_connected_workers() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(
                        new AgentPipeline.PipelineNode("a", AgentType.of("k8s")),
                        new AgentPipeline.PipelineNode("b", AgentType.of("aiops"))),
                List.of(new AgentPipeline.PipelineEdge("a", "b")));

        assertEquals(
                List.of(AgentType.of("k8s"), AgentType.of("aiops")),
                pipeline.executionOrder());
    }

    @Test
    void should_reject_empty_pipeline() {
        AgentPipeline pipeline = AgentPipeline.create(List.of(), List.of());
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, pipeline::executionOrder);
        assertTrue(error.getMessage().contains("at least one"));
    }

    @Test
    void should_reject_unconnected_nodes() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(
                        new AgentPipeline.PipelineNode("a", AgentType.of("k8s")),
                        new AgentPipeline.PipelineNode("b", AgentType.of("aiops"))),
                List.of());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, pipeline::executionOrder);
        assertTrue(error.getMessage().contains("connect"));
    }

    @Test
    void should_reject_cycle() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(
                        new AgentPipeline.PipelineNode("a", AgentType.of("k8s")),
                        new AgentPipeline.PipelineNode("b", AgentType.of("aiops"))),
                List.of(
                        new AgentPipeline.PipelineEdge("a", "b"),
                        new AgentPipeline.PipelineEdge("b", "a")));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, pipeline::executionOrder);
        assertTrue(error.getMessage().contains("cycle"));
    }

    @Test
    void should_reject_supervisor_node() {
        AgentPipeline pipeline = AgentPipeline.create(
                List.of(new AgentPipeline.PipelineNode("s", AgentType.supervisor())),
                List.of());

        assertThrows(IllegalArgumentException.class, pipeline::executionOrder);
    }
}
