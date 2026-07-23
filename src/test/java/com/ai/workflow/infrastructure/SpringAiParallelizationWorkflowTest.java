package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.ParallelizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiParallelizationWorkflow")
class SpringAiParallelizationWorkflowTest {

    @Mock
    private ChatClientProvider chatClientProvider;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SpringAiParallelizationWorkflow workflow;

    @BeforeEach
    void setUp() {
        when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        workflow = new SpringAiParallelizationWorkflow(chatClientProvider);
    }

    @Test
    void should_returnOutputs_when_itemsProcessedInParallel() {
        when(callResponseSpec.content())
                .thenReturn("fr:Hello", "fr:World");

        ParallelizationResult result = workflow.parallel(
                "Translate to French:",
                List.of("Hello", "World"),
                2);

        assertThat(result.outputs()).hasSize(2);
        assertThat(result.outputs()).containsExactlyInAnyOrder("fr:Hello", "fr:World");
    }
}
