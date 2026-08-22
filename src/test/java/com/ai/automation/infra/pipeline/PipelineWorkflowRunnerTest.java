package com.ai.automation.infra.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.pipeline.domain.model.AgentPipeline;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PipelineWorkflowRunner")
class PipelineWorkflowRunnerTest {

  private static final String BRIEF_PROMPT =
      """
            Produce a Competitive Intelligence Brief for the company in context.

            Required sections:
            ## Thesis
            ## Recommendation
            """;

  @Test
  void shouldBuildLinearPipelineWhenAgentTypesProvided() {
    AgentPipeline pipeline =
        PipelineWorkflowRunner.toLinearPipeline(List.of("research", "analyst"));

    assertThat(pipeline.nodes()).hasSize(2);
    assertThat(pipeline.edges()).hasSize(1);
    assertThat(pipeline.executionOrder())
        .extracting(node -> node.agentType().value())
        .containsExactly("research", "analyst");
  }

  @Test
  void shouldMergeTopicAndBriefPromptWhenScheduleBriefIsTopic() {
    String message =
        PipelineWorkflowRunner.resolveInvokeMessage(
            "Competitor landscape brief", "Competitor landscape brief", BRIEF_PROMPT);

    assertThat(message).startsWith("Competitor landscape brief\n\n");
    assertThat(message).contains("Competitive Intelligence Brief");
    assertThat(message).contains("## Thesis");
  }

  @Test
  void shouldUseTemplateShortTopicWhenScheduleBriefIsPlaceholder() {
    String message =
        PipelineWorkflowRunner.resolveInvokeMessage(
            PipelineWorkflowRunner.GENERIC_PLACEHOLDER, "Competitor landscape brief", BRIEF_PROMPT);

    assertThat(message).startsWith("Competitor landscape brief\n\n");
    assertThat(message).doesNotContain(PipelineWorkflowRunner.GENERIC_PLACEHOLDER);
  }

  @Test
  void shouldUseBriefPromptAloneWhenTopicAndBriefBlank() {
    String message = PipelineWorkflowRunner.resolveInvokeMessage("  ", "  ", BRIEF_PROMPT);

    assertThat(message).isEqualTo(BRIEF_PROMPT.trim());
  }

  @Test
  void shouldNotDuplicateWhenScheduleBriefAlreadyContainsInstructions() {
    String full = "Custom topic\n\n" + BRIEF_PROMPT.trim();
    String message = PipelineWorkflowRunner.resolveInvokeMessage(full, "ignored", BRIEF_PROMPT);

    assertThat(message).isEqualTo(full);
  }
}
