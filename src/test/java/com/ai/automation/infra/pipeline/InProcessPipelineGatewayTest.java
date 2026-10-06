package com.ai.automation.infra.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.pipeline.domain.model.AgentPipeline;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InProcessPipelineGateway")
class InProcessPipelineGatewayTest {

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
        InProcessPipelineGateway.toLinearPipeline(List.of("research", "analyst"));

    assertThat(pipeline.nodes()).hasSize(2);
    assertThat(pipeline.edges()).hasSize(1);
    assertThat(pipeline.resolveExecutionOrder())
        .extracting(node -> node.agentType().value())
        .containsExactly("research", "analyst");
  }

  @Test
  void shouldMergeTopicAndBriefPromptWhenScheduleBriefIsTopic() {
    String message =
        InProcessPipelineGateway.resolveInvokeMessage(
            "Competitor landscape brief", "Competitor landscape brief", BRIEF_PROMPT);

    assertThat(message).startsWith("Competitor landscape brief\n\n");
    assertThat(message).contains("Competitive Intelligence Brief");
    assertThat(message).contains("## Thesis");
  }

  @Test
  void shouldUseTemplateShortTopicWhenScheduleBriefIsPlaceholder() {
    String message =
        InProcessPipelineGateway.resolveInvokeMessage(
            InProcessPipelineGateway.GENERIC_PLACEHOLDER,
            "Competitor landscape brief",
            BRIEF_PROMPT);

    assertThat(message).startsWith("Competitor landscape brief\n\n");
    assertThat(message).doesNotContain(InProcessPipelineGateway.GENERIC_PLACEHOLDER);
  }

  @Test
  void shouldUseBriefPromptAloneWhenTopicAndBriefBlank() {
    String message = InProcessPipelineGateway.resolveInvokeMessage("  ", "  ", BRIEF_PROMPT);

    assertThat(message).isEqualTo(BRIEF_PROMPT.trim());
  }

  @Test
  void shouldNotDuplicateWhenScheduleBriefAlreadyContainsInstructions() {
    String full = "Custom topic\n\n" + BRIEF_PROMPT.trim();
    String message = InProcessPipelineGateway.resolveInvokeMessage(full, "ignored", BRIEF_PROMPT);

    assertThat(message).isEqualTo(full);
  }
}
