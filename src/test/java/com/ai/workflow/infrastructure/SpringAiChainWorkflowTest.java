package com.ai.workflow.infrastructure;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.workflow.domain.model.ChainResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiChainWorkflow")
class SpringAiChainWorkflowTest {

    @Mock
    private ChatClientProvider chatClientProvider;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private SpringAiChainWorkflow workflow;

    @BeforeEach
    void setUp() {
        when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        workflow = new SpringAiChainWorkflow(chatClientProvider);
    }

    @Test
    void should_returnFinalOutput_when_chainCompletesSequentially() {
        when(callResponseSpec.content())
                .thenReturn("step-1")
                .thenReturn("step-2");

        ChainResult result = workflow.chain("raw input", new String[]{"extract", "format"});

        assertThat(result.output()).isEqualTo("step-2");
        assertThat(result.intermediateSteps()).containsExactly("raw input", "step-1", "step-2");
        verify(chatClientProvider, times(2)).createBareStateless(any(TextChatOptions.class));
    }
}
