package com.ai.tools.application;

import com.ai.common.application.ChatClientProvider;
import com.ai.common.application.TextChatOptions;
import com.ai.common.domain.DocumentSearchTool;
import com.ai.common.domain.WebSearchTool;
import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.tools.domain.WeatherReport;
import com.ai.tools.infrastructure.WeatherTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ToolsFacade chat and search")
class ToolsFacadeChatTest {

    @Mock
    private ChatClientProvider chatClientProvider;
    @Mock
    private WeatherTools weatherTools;
    @Mock
    private WeatherReport weatherReport;
    @Mock
    private DocumentSearchTool documentSearchTool;
    @Mock
    private WebSearchTool webSearchTool;
    @Mock
    private AiInvocationRecorder invocationRecorder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ToolsFacade toolsFacade;

    @BeforeEach
    void setUp() {
        toolsFacade = new ToolsFacade(
                chatClientProvider, weatherTools, weatherReport, documentSearchTool, webSearchTool, invocationRecorder);
    }

    @Test
    @DisplayName("should_chat_with_tools_and_record_success")
    void should_chat_with_tools_and_record_success() {
        when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("answer");

        assertThat(toolsFacade.chatWithTools("what is weather?")).isEqualTo("answer");
        verify(invocationRecorder).recordSuccess(any(), anyString(), anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("should_delegate_web_search")
    void should_delegate_web_search() {
        when(webSearchTool.searchWeb("q")).thenReturn("hits");
        assertThat(toolsFacade.searchWeb("q")).isEqualTo("hits");
    }

    @Test
    @DisplayName("should_list_documents_via_document_search_tool")
    void should_list_documents_via_document_search_tool() {
        when(documentSearchTool.listDocuments()).thenReturn("doc-list");
        assertThat(toolsFacade.listDocuments()).isEqualTo("doc-list");
    }
}
