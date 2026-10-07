package com.ai.tools.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.DocumentSearchTool;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.common.service.llm.WebSearchTool;
import com.ai.metrics.domain.model.AiCapability;
import com.ai.metrics.domain.model.Latency;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.tools.domain.model.WeatherSimulator;
import com.ai.tools.infra.tools.WeatherTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolService chat and search")
class ToolServiceChatTest {

  private static final OwnerKey OWNER = OwnerKey.forClient("11111111-1111-4111-8111-111111111111");

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private WeatherTools weatherTools;
  @Mock private WeatherSimulator weatherSimulator;
  @Mock private DocumentSearchTool documentSearchTool;
  @Mock private WebSearchTool webSearchTool;
  @Mock private AiInvocationRecorder invocationRecorder;
  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  private ToolService toolService;

  @BeforeEach
  void setUp() {
    toolService =
        new ToolService(
            chatClientProvider,
            weatherTools,
            weatherSimulator,
            documentSearchTool,
            webSearchTool,
            invocationRecorder);
  }

  @Test
  @DisplayName("should chat with tools and record success")
  void shouldChatWithToolsAndRecordSuccess() {
    when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn("answer");

    assertThat(toolService.chatWithTools("what is weather?", OWNER)).isEqualTo("answer");
    verify(invocationRecorder)
        .recordSuccess(
            eq(AiCapability.TOOLS),
            anyString(),
            any(Latency.class),
            eq(OWNER),
            any(),
            any(),
            any());
  }
}
