package com.ai.automation.infrastructure.pipeline;

import com.ai.pipeline.domain.model.AgentPipeline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipelineWorkflowRunner")
class PipelineWorkflowRunnerTest {

    private static final String BRIEF_PROMPT = """
            Produce a Competitive Intelligence Brief for the company in context.

            Required sections:
            ## Thesis
            ## Recommendation
            """;

    @Test
    void should_buildLinearPipeline_when_agentTypesProvided() {
        AgentPipeline pipeline = PipelineWorkflowRunner.toLinearPipeline(List.of("research", "analyst"));

        assertThat(pipeline.nodes()).hasSize(2);
        assertThat(pipeline.edges()).hasSize(1);
        assertThat(pipeline.executionOrder())
                .extracting(node -> node.agentType().value())
                .containsExactly("research", "analyst");
    }

    @Test
    void should_mergeTopicAndBriefPrompt_when_scheduleBriefIsTopic() {
        String message = PipelineWorkflowRunner.resolveInvokeMessage(
                "Competitor landscape brief",
                "Competitor landscape brief",
                BRIEF_PROMPT);

        assertThat(message).startsWith("Competitor landscape brief\n\n");
        assertThat(message).contains("Competitive Intelligence Brief");
        assertThat(message).contains("## Thesis");
    }

    @Test
    void should_useTemplateShortTopic_when_scheduleBriefIsPlaceholder() {
        String message = PipelineWorkflowRunner.resolveInvokeMessage(
                PipelineWorkflowRunner.GENERIC_PLACEHOLDER,
                "Competitor landscape brief",
                BRIEF_PROMPT);

        assertThat(message).startsWith("Competitor landscape brief\n\n");
        assertThat(message).doesNotContain(PipelineWorkflowRunner.GENERIC_PLACEHOLDER);
    }

    @Test
    void should_useBriefPromptAlone_when_topicAndBriefBlank() {
        String message = PipelineWorkflowRunner.resolveInvokeMessage("  ", "  ", BRIEF_PROMPT);

        assertThat(message).isEqualTo(BRIEF_PROMPT.trim());
    }

    @Test
    void should_notDuplicate_when_scheduleBriefAlreadyContainsInstructions() {
        String full = "Custom topic\n\n" + BRIEF_PROMPT.trim();
        String message = PipelineWorkflowRunner.resolveInvokeMessage(full, "ignored", BRIEF_PROMPT);

        assertThat(message).isEqualTo(full);
    }
}
