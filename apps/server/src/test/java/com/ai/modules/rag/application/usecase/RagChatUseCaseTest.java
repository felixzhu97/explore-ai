package com.ai.modules.rag.application.usecase;

import com.ai.modules.rag.application.usecase.RagApplicationService;
import com.ai.modules.rag.application.usecase.RagChatUseCase;
import com.ai.modules.rag.domain.model.SourceDocument;
import com.ai.modules.ai.application.usecase.AiChatUseCase;
import com.ai.domain.service.LanguageDetectionService;
import com.ai.domain.service.PromptTemplates;
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
    private AiChatUseCase aiChatUseCase;

    @Mock
    private LanguageDetectionService languageDetectionService;

    @Mock
    private PromptTemplates promptTemplates;

    private RagChatUseCase ragChatUseCase;

    @BeforeEach
    void setUp() {
        ragChatUseCase = new RagChatUseCase(
                ragApplicationService,
                aiChatUseCase,
                languageDetectionService,
                promptTemplates
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
            when(promptTemplates.buildQuestionAnswerPrompt(context, question)).thenReturn("final prompt");
            when(ragApplicationService.retrieveContext(eq(question), isNull(), eq(5))).thenReturn(retrievalResult);
            when(aiChatUseCase.chat(anyString())).thenReturn(aiResponse);

            // Act
            RagChatUseCase.ChatResult result = ragChatUseCase.chat(question, null, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.response()).isEqualTo(aiResponse);
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().get(0).text()).isEqualTo("AI definition");

            verify(ragApplicationService).retrieveContext(question, null, 5);
            verify(aiChatUseCase).chat(anyString());
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
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString())).thenReturn("prompt");
            when(promptTemplates.buildQuestionAnswerPrompt(anyString(), anyString())).thenReturn("final prompt");
            when(ragApplicationService.retrieveContext(question, expectedDocUuids, 5)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat(anyString())).thenReturn("response");

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
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString())).thenReturn("prompt");
            when(promptTemplates.buildQuestionAnswerPrompt(anyString(), anyString())).thenReturn("final prompt");
            when(ragApplicationService.retrieveContext(question, null, customTopK)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat(anyString())).thenReturn("response");

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
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString())).thenReturn("prompt");
            when(promptTemplates.buildQuestionAnswerPrompt(anyString(), anyString())).thenReturn("final prompt");
            when(ragApplicationService.retrieveContext(question, null, expectedDefaultTopK)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat(anyString())).thenReturn("response");

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
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString())).thenReturn("prompt");
            when(promptTemplates.buildQuestionAnswerPrompt(anyString(), anyString())).thenReturn("final prompt");
            when(ragApplicationService.retrieveContext(question, null, 5)).thenReturn(retrievalResult);
            when(aiChatUseCase.chat(anyString())).thenReturn("response");

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
        @DisplayName("should detect language and build prompt")
        void shouldDetectLanguageAndBuildPrompt() {
            // Arrange
            String question = "什么是AI？";
            String context = "AI是人工智能";
            String languageCode = "zh";
            String expectedPrompt = "Prompt with context and question";

            RagApplicationService.RetrievalResult retrievalResult =
                    new RagApplicationService.RetrievalResult(context, Collections.emptyList(), question);

            when(languageDetectionService.detect(question)).thenReturn(languageCode);
            when(languageDetectionService.buildPrompt(question, context, languageCode)).thenReturn("built prompt");
            when(promptTemplates.buildQuestionAnswerPrompt(context, question)).thenReturn(expectedPrompt);
            when(ragApplicationService.retrieveContext(eq(question), isNull(), eq(5))).thenReturn(retrievalResult);
            when(aiChatUseCase.chat(expectedPrompt)).thenReturn("response");

            // Act
            RagChatUseCase.ChatResult result = ragChatUseCase.chat(question, null, null);

            // Assert
            assertThat(result).isNotNull();
            verify(languageDetectionService).detect(question);
            verify(languageDetectionService).buildPrompt(question, context, languageCode);
            verify(promptTemplates).buildQuestionAnswerPrompt(context, question);
        }
    }
}
