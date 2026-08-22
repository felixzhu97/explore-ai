package com.ai.tools.service.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.tools.domain.model.WeatherReport;
import com.ai.tools.infra.tools.WeatherTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolsFacade chat and search")
class ToolsFacadeChatTest {

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private WeatherTools weatherTools;
  @Mock private WeatherReport weatherReport;
  @Mock private DocumentSearchTool documentSearchTool;
  @Mock private WebSearchTool webSearchTool;
  @Mock private AiInvocationRecorder invocationRecorder;
  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  private ToolsFacade toolsFacade;

  @BeforeEach
  void setUp() {
    toolsFacade =
        new ToolsFacade(
            chatClientProvider,
            weatherTools,
            weatherReport,
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

    assertThat(toolsFacade.chatWithTools("what is weather?")).isEqualTo("answer");
    verify(invocationRecorder).recordSuccess(any(), anyString(), anyLong(), any(), any(), any());
  }

  @Test
  @DisplayName("should delegate web search")
  void shouldDelegateWebSearch() {
    when(webSearchTool.searchWeb("q")).thenReturn("hits");
    assertThat(toolsFacade.searchWeb("q")).isEqualTo("hits");
  }

  @Test
  @DisplayName("should list documents via document search tool")
  void shouldListDocumentsViaDocumentSearchTool() {
    when(documentSearchTool.listDocuments()).thenReturn("doc-list");
    assertThat(toolsFacade.listDocuments()).isEqualTo("doc-list");
  }
}
