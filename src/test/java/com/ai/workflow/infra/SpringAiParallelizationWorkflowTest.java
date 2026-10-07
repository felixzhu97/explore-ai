package com.ai.workflow.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.workflow.domain.model.ParallelizationResult;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiParallelizationWorkflow")
class SpringAiParallelizationWorkflowTest {

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;

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
  void shouldReturnOutputsWhenItemsProcessedInParallel() {
    when(callResponseSpec.content()).thenReturn("fr:Hello", "fr:World");

    ParallelizationResult result =
        workflow.runParallel("Translate to French:", List.of("Hello", "World"), 2);

    assertThat(result.getOutputs()).hasSize(2);
    assertThat(result.getOutputs()).containsExactlyInAnyOrder("fr:Hello", "fr:World");
  }

  @Test
  @DisplayName("should cap concurrent calls when requested parallelism is too high")
  void shouldCapConcurrentCallsWhenRequestedParallelismIsTooHigh() {
    AtomicInteger running = new AtomicInteger();
    AtomicInteger peak = new AtomicInteger();
    when(callResponseSpec.content())
        .thenAnswer(
            invocation -> {
              peak.accumulateAndGet(running.incrementAndGet(), Math::max);
              Thread.sleep(20);
              running.decrementAndGet();
              return "ok";
            });
    List<String> items = IntStream.range(0, 16).mapToObj(String::valueOf).toList();

    ParallelizationResult result = workflow.runParallel("Echo:", items, 1_000);

    assertThat(result.getOutputs()).hasSize(16);
    assertThat(peak.get()).isLessThanOrEqualTo(8);
  }
}
