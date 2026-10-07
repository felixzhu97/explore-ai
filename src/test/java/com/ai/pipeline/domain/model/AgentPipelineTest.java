package com.ai.pipeline.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentPipeline")
class AgentPipelineTest {

  @Test
  void shouldOrderSingleNodeWithoutEdges() {
    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(AgentPipeline.PipelineNode.of("n1", AgentType.of("k8s"))), List.of());

    assertEquals(
        List.of(AgentType.of("k8s")),
        pipeline.resolveExecutionOrder().stream()
            .map(AgentPipeline.PipelineNode::agentType)
            .toList());
  }

  @Test
  void shouldTopoSortConnectedWorkers() {
    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(
                AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
            List.of(new AgentPipeline.PipelineEdge("a", "b")));

    assertEquals(
        List.of(AgentType.of("k8s"), AgentType.of("aiops")),
        pipeline.resolveExecutionOrder().stream()
            .map(AgentPipeline.PipelineNode::agentType)
            .toList());
  }

  @Test
  void shouldRejectEmptyPipeline() {
    AgentPipeline pipeline = AgentPipeline.create(List.of(), List.of());
    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, pipeline::resolveExecutionOrder);
    assertTrue(error.getMessage().contains("at least one"));
  }

  @Test
  void shouldRejectUnconnectedNodes() {
    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(
                AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
            List.of());

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, pipeline::resolveExecutionOrder);
    assertTrue(error.getMessage().contains("connect"));
  }

  @Test
  void shouldRejectCycle() {
    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(
                AgentPipeline.PipelineNode.of("a", AgentType.of("k8s")),
                AgentPipeline.PipelineNode.of("b", AgentType.of("aiops"))),
            List.of(
                new AgentPipeline.PipelineEdge("a", "b"),
                new AgentPipeline.PipelineEdge("b", "a")));

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, pipeline::resolveExecutionOrder);
    assertTrue(error.getMessage().contains("cycle"));
  }

  @Test
  void shouldRejectSupervisorNode() {
    AgentPipeline pipeline =
        AgentPipeline.create(
            List.of(AgentPipeline.PipelineNode.of("s", AgentType.supervisor())), List.of());

    assertThrows(IllegalArgumentException.class, pipeline::resolveExecutionOrder);
  }

  @Test
  void shouldKeepNodeSnapshotFields() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode(
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

  @Test
  @DisplayName("should use the catalog prompt when a pipeline node has no prompt of its own")
  void shouldUseTheCatalogPromptWhenAPipelineNodeHasNoPromptOfItsOwn() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode("n0", AgentType.of("research"), "Scout", "", "", List.of());

    AgentDefinition definition = node.toDefinition(builtin("research"));

    assertFalse(node.hasOwnPrompt());
    assertEquals("Scout", definition.name());
    assertEquals("You are research.", definition.systemPrompt());
  }

  @Test
  @DisplayName("should keep the node prompt when a pipeline node has its own prompt")
  void shouldKeepTheNodePromptWhenAPipelineNodeHasItsOwnPrompt() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode(
            "n0", AgentType.of("research"), "Scout", "", "Only cite sources.", List.of());

    assertEquals("Only cite sources.", node.toDefinition(builtin("research")).systemPrompt());
  }

  private static AgentDefinition builtin(String type) {
    return AgentDefinition.create(
        AgentType.of(type),
        "Builtin " + type,
        "",
        "You are " + type + ".",
        List.of(),
        AgentDefinition.RUNTIME_SINGLE);
  }
}
