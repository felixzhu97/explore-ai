package com.ai.rag.application.usecase;

import com.ai.rag.application.usecase.RagApplicationService;
import com.ai.rag.application.usecase.RagChatUseCase;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.ai.application.usecase.ChatUseCase;
import com.ai.ai.domain.service.LanguageDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatUseCase")
class RagChatUseCaseTest {

    @Mock
    private RagApplicationService ragApplicationService;

    @Mock
    private ChatUseCase aiChatUseCase;

    @Mock
    private LanguageDetectionService languageDetectionService;

    private RagChatUseCase ragChatUseCase;

    @BeforeEach
    void setUp() {
        ragChatUseCase = new RagChatUseCase(
                ragApplicationService,
                aiChatUseCase,
                languageDetectionService
        );
    }

    @Nested
    @DisplayName("chat()")
    class Chat {

        @Test
        @DisplayName("should return ChatResult with response and sources")
        void shouldReturnChatResultWithResponseAndSources() {
            // Arrange
            String question = "What is AI?";
            String context = "AI stands for Artificial Intelligence";
            String aiResponse = "AI is Artificial Intelligence";
            List<SourceDocument> sources = List.of(
                    new SourceDocument("AI definition", 0.95, Map.of())
            );
            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult(context, sources, question);

            when(languageDetectionService.detect(question)).thenReturn("en");
            when(languageDetectionService.buildPrompt(question, context, "en")).thenReturn("built prompt");
            when(ragApplicationService.retrieveContext(eq(question), isNull(), eq(5))).thenReturn(retrievalResult);
            when(aiChatUseCase.chat("built prompt")).thenReturn(aiResponse);

            // Act
            RagChatUseCase.ChatResult result = ragChatUseCase.chat(question, null, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.response()).isEqualTo(aiResponse);
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).text()).isEqualTo("AI definition");

            verify(ragApplicationService).retrieveContext(question, null, 5);
            verify(aiChatUseCase).chat("built prompt");
        }

        @Test
        @DisplayName("should pass docIds to service when provided")
        void shouldPassDocIdsToServiceWhenProvided() {
            // Arrange
            String question = "What is AI?";
            String docId1 = UUID.randomUUID().toString();
            List<String> docIds = List.of(docId1);
            List<UUID> expectedDocUuids = List.of(UUID.fromString(docId1));

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(languageDetectionService.detect(question)).thenReturn("en");
            when(languageDetectionService.buildPrompt(eq(question), eq("context"), eq("en"))).thenReturn("prompt");
            when(ragApplicationService.retrieveContext(question, expectedDocUuids, 5)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat("prompt")).thenReturn("response");

            // Act
            ragChatUseCase.chat(question, docIds, null);

            // Assert
            verify(ragApplicationService).retrieveContext(question, expectedDocUuids, 5);
        }

        @Test
        @DisplayName("should use custom topK when provided")
        void shouldUseCustomTopKWhenProvided() {
            // Arrange
            String question = "What is AI?";
            int customTopK = 10;

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(languageDetectionService.detect(question)).thenReturn("en");
            when(languageDetectionService.buildPrompt(eq(question), eq("context"), eq("en"))).thenReturn("prompt");
            when(ragApplicationService.retrieveContext(question, null, customTopK)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat("prompt")).thenReturn("response");

            // Act
            ragChatUseCase.chat(question, null, customTopK);

            // Assert
            verify(ragApplicationService).retrieveContext(question, null, customTopK);
        }

        @Test
        @DisplayName("should use default topK value of 5 when topK is null")
        void shouldUseDefaultTopKWhenTopKIsNull() {
            // Arrange
            String question = "What is AI?";
            int expectedDefaultTopK = 5;

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(languageDetectionService.detect(question)).thenReturn("en");
            when(languageDetectionService.buildPrompt(eq(question), eq("context"), eq("en"))).thenReturn("prompt");
            when(ragApplicationService.retrieveContext(question, null, expectedDefaultTopK)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat("prompt")).thenReturn("response");

            // Act
            ragChatUseCase.chat(question, null, null);

            // Assert
            verify(ragApplicationService).retrieveContext(question, null, expectedDefaultTopK);
        }

        @Test
        @DisplayName("should handle empty docIds list")
        void shouldHandleEmptyDocIdsList() {
            // Arrange
            String question = "What is AI?";
            List<String> emptyDocIds = Collections.emptyList();

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult("context", Collections.emptyList(), question);

            when(languageDetectionService.detect(question)).thenReturn("en");
            when(languageDetectionService.buildPrompt(eq(question), eq("context"), eq("en"))).thenReturn("prompt");
            when(ragApplicationService.retrieveContext(question, null, 5)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat("prompt")).thenReturn("response");

            // Act
            ragChatUseCase.chat(question, emptyDocIds, null);

            // Assert
            verify(ragApplicationService).retrieveContext(question, null, 5);
        }
    }

    @Nested
    @DisplayName("buildPrompt")
    class BuildPrompt {

        @Test
        @DisplayName("should detect language and build prompt using language detection service")
        void shouldDetectLanguageAndBuildPromptUsingLanguageDetectionService() {
            // Arrange
            String question = "什么是AI？";
            String context = "AI是人工智能";
            String languageCode = "zh";
            String expectedPrompt = "built prompt with language support";

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult(context, Collections.emptyList(), question);

            when(languageDetectionService.detect(question)).thenReturn(languageCode);
            when(languageDetectionService.buildPrompt(question, context, languageCode)).thenReturn(expectedPrompt);
            when(ragApplicationService.retrieveContext(eq(question), isNull(), eq(5))).thenReturn(retrievalResult);
            when(aiChatUseCase.chat(expectedPrompt)).thenReturn("response");

            // Act
            RagChatUseCase.ChatResult result = ragChatUseCase.chat(question, null, null);

            // Assert
            assertThat(result).isNotNull();
            verify(languageDetectionService).detect(question);
            verify(languageDetectionService).buildPrompt(question, context, languageCode);
        }
    }
}
