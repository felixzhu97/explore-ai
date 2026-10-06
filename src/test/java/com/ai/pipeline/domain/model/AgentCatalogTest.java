package com.ai.pipeline.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.pipeline.domain.vo.AgentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentCatalog")
class AgentCatalogTest {

  private static final String OWNER = "c:11111111-1111-1111-1111-111111111111";

  private static AgentDefinition builtin(String type) {
    return AgentDefinition.create(
        AgentType.of(type),
        "Builtin " + type,
        "",
        "You are " + type + ".",
        List.of(),
        AgentDefinition.RUNTIME_SINGLE);
  }

  @Test
  @DisplayName("should let a saved agent replace the built-in agent of the same type")
  void shouldLetASavedAgentReplaceTheBuiltInAgentOfTheSameType() {
    SavedAgent mine = SavedAgent.create(OWNER, "research", "My research", "", "Dig deep.", null);

    List<AgentDefinition> merged =
        AgentCatalog.merge(List.of(builtin("research"), builtin("analyst")), List.of(mine));

    assertThat(merged)
        .extracting(AgentDefinition::name)
        .containsExactly("My research", "Builtin analyst");
  }

  @Test
  @DisplayName("should append saved agents with new types after the built-in agents")
  void shouldAppendSavedAgentsWithNewTypesAfterTheBuiltInAgents() {
    SavedAgent extra = SavedAgent.create(OWNER, "legal", "Legal", "", "Check contracts.", null);

    List<AgentDefinition> merged = AgentCatalog.merge(List.of(builtin("research")), List.of(extra));

    assertThat(merged)
        .extracting(definition -> definition.type().value())
        .containsExactly("research", "legal");
    assertThat(extra.hasType(AgentType.of(" Legal "))).isTrue();
    assertThat(extra.hasType(AgentType.of("research"))).isFalse();
  }

  @Test
  @DisplayName("should use the catalog prompt when a pipeline node has no prompt of its own")
  void shouldUseTheCatalogPromptWhenAPipelineNodeHasNoPromptOfItsOwn() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode("n0", AgentType.of("research"), "Scout", "", "", List.of());

    AgentDefinition definition = node.toDefinition(builtin("research"));

    assertThat(node.hasOwnPrompt()).isFalse();
    assertThat(definition.name()).isEqualTo("Scout");
    assertThat(definition.systemPrompt()).isEqualTo("You are research.");
  }

  @Test
  @DisplayName("should keep the node prompt when a pipeline node has its own prompt")
  void shouldKeepTheNodePromptWhenAPipelineNodeHasItsOwnPrompt() {
    AgentPipeline.PipelineNode node =
        new AgentPipeline.PipelineNode(
            "n0", AgentType.of("research"), "Scout", "", "Only cite sources.", List.of());

    assertThat(node.toDefinition(builtin("research")).systemPrompt())
        .isEqualTo("Only cite sources.");
  }
}
