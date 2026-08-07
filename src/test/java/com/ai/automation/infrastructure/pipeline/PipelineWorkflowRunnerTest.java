package com.ai.automation.infrastructure.pipeline;

import com.ai.pipeline.domain.model.AgentPipeline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipelineWorkflowRunner")
class PipelineWorkflowRunnerTest {

    @Test
    void should_buildLinearPipeline_when_agentTypesProvided() {
        AgentPipeline pipeline = PipelineWorkflowRunner.toLinearPipeline(List.of("research", "analyst"));

        assertThat(pipeline.nodes()).hasSize(2);
        assertThat(pipeline.edges()).hasSize(1);
        assertThat(pipeline.executionOrder())
                .extracting(node -> node.agentType().value())
                .containsExactly("research", "analyst");
    }
}
