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
        AgentPipeline.createPipeline(
            List.of(AgentPipeline.PipelineNode.createNode("n1", AgentType.createType("k8s"))),
            List.of());

    assertEquals(
        List.of(AgentType.createType("k8s")),
        pipeline.resolveExecutionOrder().stream()
            .map(AgentPipeline.PipelineNode::getAgentType)
            .toList());
  }

  @Test
  void shouldTopoSortConnectedWorkers() {
    AgentPipeline pipeline =
        AgentPipeline.createPipeline(
            List.of(
                AgentPipeline.PipelineNode.createNode("a", AgentType.createType("k8s")),
                AgentPipeline.PipelineNode.createNode("b", AgentType.createType("aiops"))),
            List.of(new AgentPipeline.PipelineEdge("a", "b")));

    assertEquals(
        List.of(AgentType.createType("k8s"), AgentType.createType("aiops")),
        pipeline.resolveExecutionOrder().stream()
            .map(AgentPipeline.PipelineNode::getAgentType)
            .toList());
  }

  @Test
  void shouldRejectEmptyPipeline() {
    AgentPipeline pipeline = AgentPipeline.createPipeline(List.of(), List.of());
    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, pipeline::resolveExecutionOrder);
    assertTrue(error.getMessage().contains("at least one"));
  }

  @Test
  void shouldRejectUnconnectedNodes() {
    AgentPipeline pipeline =
        AgentPipeline.createPipeline(
            List.of(
                AgentPipeline.PipelineNode.createNode("a", AgentType.createType("k8s")),
                AgentPipeline.PipelineNode.createNode("b", AgentType.createType("aiops"))),
            List.of());

    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, pipeline::resolveExecutionOrder);
    assertTrue(error.getMessage().contains("connect"));
  }

  @Test
  void shouldRejectCycle() {
    AgentPipeline pipeline =
        AgentPipeline.createPipeline(
            List.of(
                AgentPipeline.PipelineNode.createNode("a", AgentType.createType("k8s")),
                AgentPipeline.PipelineNode.createNode("b", AgentType.createType("aiops"))),
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
        AgentPipeline.createPipeline(
            List.of(AgentPipeline.PipelineNode.createNode("s", AgentType.createSupervisorType())),
            List.of());

    assertThrows(IllegalArgumentException.class, pipeline::resolveExecutionOrder);
  }

  @Test
  void shouldKeepNodeSnapshotFields() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode(
            "n1",
            AgentType.createType("research"),
            "Custom Research",
            "custom desc",
            "Custom prompt",
            List.of("web_search"));

    assertEquals("Custom Research", node.getName());
    assertEquals("Custom prompt", node.getSystemPrompt());
    assertEquals(List.of("web_search"), node.getTools());
    assertEquals("Custom prompt", node.buildDefinition().getSystemPrompt());
  }

  @Test
  @DisplayName("should use the catalog prompt when a pipeline node has no prompt of its own")
  void shouldUseTheCatalogPromptWhenAPipelineNodeHasNoPromptOfItsOwn() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode(
            "n0", AgentType.createType("research"), "Scout", "", "", List.of());

    AgentDefinition definition = node.buildDefinition(builtin("research"));

    assertFalse(node.hasOwnPrompt());
    assertEquals("Scout", definition.getName());
    assertEquals("You are research.", definition.getSystemPrompt());
  }

  @Test
  @DisplayName("should keep the node prompt when a pipeline node has its own prompt")
  void shouldKeepTheNodePromptWhenAPipelineNodeHasItsOwnPrompt() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode(
            "n0", AgentType.createType("research"), "Scout", "", "Only cite sources.", List.of());

    assertEquals("Only cite sources.", node.buildDefinition(builtin("research")).getSystemPrompt());
  }

  private static AgentDefinition builtin(String type) {
    return AgentDefinition.createDefinition(
        AgentType.createType(type),
        "Builtin " + type,
        "",
        "You are " + type + ".",
        List.of(),
        AgentDefinition.RUNTIME_SINGLE);
  }
}
