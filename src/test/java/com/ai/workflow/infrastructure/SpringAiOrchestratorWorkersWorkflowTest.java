package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.OrchestratorWorkersResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiOrchestratorWorkersWorkflow")
class SpringAiOrchestratorWorkersWorkflowTest {

    @Mock
    private ChatClientProvider chatClientProvider;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SpringAiOrchestratorWorkersWorkflow workflow;

    @BeforeEach
    void setUp() {
        when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        workflow = new SpringAiOrchestratorWorkersWorkflow(chatClientProvider);
    }

    @Test
    void should_returnAnalysisWorkersAndSynthesis_when_processCompletes() {
        when(callResponseSpec.entity(eq(SpringAiOrchestratorWorkersWorkflow.OrchestratorPlan.class)))
                .thenReturn(new SpringAiOrchestratorWorkersWorkflow.OrchestratorPlan(
                        "two styles",
                        List.of(
                                new SpringAiOrchestratorWorkersWorkflow.PlannedTask("formal", "precise"),
                                new SpringAiOrchestratorWorkersWorkflow.PlannedTask("friendly", "warm"))));

        AtomicInteger contentCalls = new AtomicInteger();
        when(callResponseSpec.content()).thenAnswer(inv -> {
            int n = contentCalls.incrementAndGet();
            return switch (n) {
                case 1, 2 -> "worker-" + n;
                default -> "synthesized answer";
            };
        });

        OrchestratorWorkersResult result = workflow.process("Write a product blurb");

        assertThat(result.analysis()).isEqualTo("two styles");
        assertThat(result.tasks()).hasSize(2);
        assertThat(result.workerResponses()).containsExactlyInAnyOrder("worker-1", "worker-2");
        assertThat(result.synthesis()).isEqualTo("synthesized answer");
    }
}
