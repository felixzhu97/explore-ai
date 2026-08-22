package com.ai.workflow.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.workflow.domain.model.RoutingResult;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiRoutingWorkflow")
class SpringAiRoutingWorkflowTest {

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  private SpringAiRoutingWorkflow workflow;

  @BeforeEach
  void setUp() {
    when(chatClientProvider.createBareStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    workflow = new SpringAiRoutingWorkflow(chatClientProvider);
  }

  @Test
  void shouldReturnSpecializedOutputWhenRouteClassified() {
    when(callResponseSpec.entity(eq(SpringAiRoutingWorkflow.RouteClassification.class)))
        .thenReturn(new SpringAiRoutingWorkflow.RouteClassification("billing keywords", "billing"));
    when(callResponseSpec.content()).thenReturn("Invoice help answer");

    RoutingResult result =
        workflow.route(
            "I was charged twice",
            Map.of(
                "billing", "You are a billing specialist.",
                "tech", "You are a tech specialist."));

    assertThat(result.selection()).isEqualTo("billing");
    assertThat(result.reasoning()).isEqualTo("billing keywords");
    assertThat(result.output()).isEqualTo("Invoice help answer");
  }
}
