package com.ai.pipeline.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PipelineTemplate")
class PipelineTemplateTest {

  private static final String OWNER = "c:11111111-1111-1111-1111-111111111111";

  @Test
  @DisplayName("should keep worker agent types in order when created")
  void shouldKeepWorkerAgentTypesInOrderWhenCreated() {
    PipelineTemplate template = create(List.of(" Research ", "analyst"));

    assertThat(template.getAgentTypes()).containsExactly("research", "analyst");
  }

  @Test
  @DisplayName("should reject a supervisor node when created")
  void shouldRejectASupervisorNodeWhenCreated() {
    assertThatThrownBy(() -> create(List.of("supervisor", "analyst")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("should reject a supervisor node when updated")
  void shouldRejectASupervisorNodeWhenUpdated() {
    PipelineTemplate template = create(List.of("research"));

    assertThatThrownBy(
            () -> template.update("Daily", null, List.of("Supervisor"), null, "Summarize news"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(template.getAgentTypes()).containsExactly("research");
  }

  @Test
  @DisplayName("should run the agents one after another when built as a linear pipeline")
  void shouldRunTheAgentsOneAfterAnotherWhenBuiltAsALinearPipeline() {
    AgentPipeline pipeline = create(List.of("research", "analyst")).toLinearPipeline();

    assertThat(pipeline.nodes()).hasSize(2);
    assertThat(pipeline.edges()).hasSize(1);
    assertThat(pipeline.resolveExecutionOrder())
        .extracting(node -> node.agentType().value())
        .containsExactly("research", "analyst");
  }

  @Test
  @DisplayName("should not be runnable when the template is disabled")
  void shouldNotBeRunnableWhenTheTemplateIsDisabled() {
    PipelineTemplate template = create(List.of("research"));
    assertThat(template.isRunnable()).isTrue();

    template.disable();

    assertThat(template.isRunnable()).isFalse();
  }

  @Nested
  @DisplayName("composeInvokeMessage")
  class ComposeInvokeMessage {

    private static final String BRIEF_PROMPT =
        """
            Produce a Competitive Intelligence Brief for the company in context.

            Required sections:
            ## Thesis
            ## Recommendation
            """;

    private final PipelineTemplate template =
        PipelineTemplate.create(
            OWNER,
            "Competitor brief",
            null,
            List.of("research"),
            "Competitor landscape brief",
            BRIEF_PROMPT,
            null);

    @Test
    @DisplayName("should put the topic before the instructions when the brief is a topic")
    void shouldPutTheTopicBeforeTheInstructionsWhenTheBriefIsATopic() {
      String message = template.composeInvokeMessage("Market entry brief");

      assertThat(message).startsWith("Market entry brief\n\n");
      assertThat(message).contains("Competitive Intelligence Brief").contains("## Thesis");
    }

    @Test
    @DisplayName("should use the template short topic when the brief is the generic text")
    void shouldUseTheTemplateShortTopicWhenTheBriefIsTheGenericText() {
      String message = template.composeInvokeMessage(PipelineTemplate.GENERIC_BRIEF);

      assertThat(message).startsWith("Competitor landscape brief\n\n");
      assertThat(message).doesNotContain(PipelineTemplate.GENERIC_BRIEF);
    }

    @Test
    @DisplayName("should use the instructions alone when topic and brief are blank")
    void shouldUseTheInstructionsAloneWhenTopicAndBriefAreBlank() {
      PipelineTemplate noTopic =
          PipelineTemplate.create(
              OWNER, "No topic", null, List.of("research"), "  ", BRIEF_PROMPT, null);

      assertThat(noTopic.composeInvokeMessage("  ")).isEqualTo(BRIEF_PROMPT.trim());
    }

    @Test
    @DisplayName("should not repeat the instructions when the brief already contains them")
    void shouldNotRepeatTheInstructionsWhenTheBriefAlreadyContainsThem() {
      String full = "Custom topic\n\n" + BRIEF_PROMPT.trim();

      assertThat(template.composeInvokeMessage(full)).isEqualTo(full);
    }
  }

  private static PipelineTemplate create(List<String> agentTypes) {
    return PipelineTemplate.create(OWNER, "Daily", null, agentTypes, null, "Summarize news", null);
  }
}
