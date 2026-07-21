package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.EvaluatorOptimizerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiEvaluatorOptimizerWorkflow")
class SpringAiEvaluatorOptimizerWorkflowTest {

    @Mock
    private ChatClientProvider chatClientProvider;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SpringAiEvaluatorOptimizerWorkflow workflow;

    @BeforeEach
    void setUp() {
        when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        workflow = new SpringAiEvaluatorOptimizerWorkflow(chatClientProvider);
    }

    @Test
    void should_returnSolutionAndChainOfThought_when_evaluationPasses() {
        when(callResponseSpec.entity(eq(SpringAiEvaluatorOptimizerWorkflow.GenerationEntity.class)))
                .thenReturn(new SpringAiEvaluatorOptimizerWorkflow.GenerationEntity(
                        "first draft", "class Solution {}"));
        when(callResponseSpec.entity(eq(SpringAiEvaluatorOptimizerWorkflow.EvaluationEntity.class)))
                .thenReturn(new SpringAiEvaluatorOptimizerWorkflow.EvaluationEntity(
                        SpringAiEvaluatorOptimizerWorkflow.EvaluationStatus.PASS,
                        "looks good"));

        EvaluatorOptimizerResult result = workflow.loop("Implement a counter");

        assertThat(result.solution()).isEqualTo("class Solution {}");
        assertThat(result.chainOfThought()).hasSize(1);
        assertThat(result.chainOfThought().getFirst().thoughts()).isEqualTo("first draft");
    }
}
