package com.ai.rag.application.usecase;

import com.ai.chat.application.usecase.TextChatOptions;
import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.chat.application.usecase.ChatClientProvider;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatUseCase")
class RagChatUseCaseTest {

    @Mock
    private RagApplicationService ragApplicationService;

    @Mock
    private ChatClientProvider chatClientProvider;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private LanguageDetectionService languageDetectionService;

    private RagChatUseCase ragChatUseCase;

    @BeforeEach
    void setUp() {
        ragChatUseCase = new RagChatUseCase(
                ragApplicationService,
                chatClientProvider,
                languageDetectionService
        );
        when(chatClientProvider.createStateless(any(TextChatOptions.class))).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(languageDetectionService.detect(anyString())).thenReturn("en");
        when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> "prompt:" + invocation.getArgument(0));
    }

    @Nested
    @DisplayName("chat()")
    class Chat {

        @Test
        @DisplayName("should return ChatResult with response and sources")
        void shouldReturnChatResultWithResponseAndSources() {
            String question = "What is AI?";
            String aiResponse = "AI is Artificial Intelligence";
            List<SourceDocument> sources = List.of(
                    new SourceDocument("AI definition", 0.95, Map.of())
            );
            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", sources, question);

            when(ragApplicationService.retrieveContext(eq(question), isNull(), eq(5))).thenReturn(retrievalResult);
            when(callResponseSpec.content()).thenReturn(aiResponse);

            RagChatUseCase.ChatResult result = ragChatUseCase.chat(question, null, null);

            assertThat(result).isNotNull();
            assertThat(result.response()).isEqualTo(aiResponse);
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().getFirst().text()).isEqualTo("AI definition");

            verify(ragApplicationService).retrieveContext(question, null, 5);
            verify(languageDetectionService).buildPrompt(question, "context", "en");
            verify(requestSpec).user("prompt:" + question);
        }

        @Test
        @DisplayName("should pass docIds to service when provided")
        void shouldPassDocIdsToServiceWhenProvided() {
            String question = "What is AI?";
            String docId1 = UUID.randomUUID().toString();
            List<String> docIds = List.of(docId1);
            List<DocumentId> expectedDocIds = List.of(DocumentId.of(docId1));

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(ragApplicationService.retrieveContext(question, expectedDocIds, 5)).thenReturn(retrievalResult);
            when(callResponseSpec.content()).thenReturn("response");

            ragChatUseCase.chat(question, docIds, null);

            verify(ragApplicationService).retrieveContext(question, expectedDocIds, 5);
        }

        @Test
        @DisplayName("should use custom topK when provided")
        void shouldUseCustomTopKWhenProvided() {
            String question = "What is AI?";
            int customTopK = 10;

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(ragApplicationService.retrieveContext(question, null, customTopK)).thenReturn(retrievalResult);
            when(callResponseSpec.content()).thenReturn("response");

            ragChatUseCase.chat(question, null, customTopK);

            verify(ragApplicationService).retrieveContext(question, null, customTopK);
        }

        @Test
        @DisplayName("should use default topK value of 5 when topK is null")
        void shouldUseDefaultTopKWhenTopKIsNull() {
            String question = "What is AI?";
            int expectedDefaultTopK = 5;

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(ragApplicationService.retrieveContext(question, null, expectedDefaultTopK)).thenReturn(retrievalResult);
            when(callResponseSpec.content()).thenReturn("response");

            ragChatUseCase.chat(question, null, null);

            verify(ragApplicationService).retrieveContext(question, null, expectedDefaultTopK);
        }

        @Test
        @DisplayName("should handle empty docIds list")
        void shouldHandleEmptyDocIdsList() {
            String question = "What is AI?";
            List<String> emptyDocIds = Collections.emptyList();

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(ragApplicationService.retrieveContext(question, null, 5)).thenReturn(retrievalResult);
            when(callResponseSpec.content()).thenReturn("response");

            ragChatUseCase.chat(question, emptyDocIds, null);

            verify(ragApplicationService).retrieveContext(question, null, 5);
        }
    }
}
