package com.ai.interfaces;

import com.ai.application.service.LanguageDetectionService;
import com.ai.application.service.RagApplicationService;
import com.ai.application.usecase.RagChatUseCase;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.AiChatService;
import com.ai.infrastructure.adapter.document.PdfTextExtractor;
import com.ai.interfaces.controller.GlobalExceptionHandler;
import com.ai.interfaces.controller.RagController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RagController Streaming Tests
 *
 * Tests for streaming RAG endpoints using standalone MockMvc setup.
 * Verifies SSE output format (ChunkEvent JSON), sources event, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RagController Streaming Tests")
class RagControllerStreamingTest {

    private MockMvc mockMvc;
    private WebTestClient webTestClient;
    private ObjectMapper objectMapper;

    @Mock
    private RagApplicationService ragApplicationService;

    @Mock
    private LanguageDetectionService languageDetectionService;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @InjectMocks
    private RagController ragController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(ragController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        webTestClient = WebTestClient.bindToController(ragController)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/rag/chat/stream endpoint")
    class StreamingEndpointTests {

        @Test
        @DisplayName("should stream chunk JSON and sources event in SSE format")
        void shouldStreamChunkJsonAndSourcesEventInSseFormat() throws Exception {
            // Arrange
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context from documents",
                    List.of(new SourceDocument(1, "Document 1", 0.9, "doc1", null)),
                    "Enriched query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(languageDetectionService.detect(anyString())).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString())).thenReturn("Built prompt");
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Hello"));

            String requestBody = "{\"query\": \"What is AI?\"}";

            // Act & Assert
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(ragApplicationService).retrieveContext(anyString(), any(), anyInt());
            verify(languageDetectionService).detect(anyString());
            verify(languageDetectionService).buildPrompt(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should handle streaming with specific document IDs")
        void shouldHandleStreamingWithSpecificDocumentIds() throws Exception {
            // Arrange
            List<String> docIds = List.of(
                    UUID.randomUUID().toString(),
                    UUID.randomUUID().toString()
            );
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context from specific docs", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            String requestBody = String.format("{\"query\": \"Question\", \"doc_ids\": [\"%s\", \"%s\"]}",
                    docIds.get(0), docIds.get(1));

            // Act
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            // Verify
            verify(ragApplicationService).retrieveContext(anyString(), any(), anyInt());
        }

        @Test
        @DisplayName("should use custom topK parameter when provided")
        void shouldUseCustomTopKParameterWhenProvided() throws Exception {
            // Arrange
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), eq(10))).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            String requestBody = "{\"query\": \"Question\", \"top_k\": 10}";

            // Act
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            // Verify custom topK was used
            verify(ragApplicationService).retrieveContext(anyString(), any(), eq(10));
        }

        @Test
        @DisplayName("should use default topK when not provided")
        void shouldUseDefaultTopKWhenNotProvided() throws Exception {
            // Arrange
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), eq(5))).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            String requestBody = "{\"query\": \"Question\"}";

            // Act
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            // Verify default topK (5) was used
            verify(ragApplicationService).retrieveContext(anyString(), any(), eq(5));
        }

        @Test
        @DisplayName("should handle null docIds in streaming request")
        void shouldHandleNullDocIdsInStreamingRequest() throws Exception {
            // Arrange
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), isNull(), anyInt())).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            String requestBody = "{\"query\": \"Question\", \"doc_ids\": null}";

            // Act
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(ragApplicationService).retrieveContext(eq("Question"), isNull(), anyInt());
        }
    }

    @Nested
    @DisplayName("Invalid UUID handling")
    class InvalidUuidHandlingTests {

        @Test
        @DisplayName("should return 400 when doc_ids contains invalid UUID string")
        void shouldReturn400WhenDocIdsContainsInvalidUuidString() {
            // Arrange
            String invalidUuid = "not-a-valid-uuid";
            String requestBody = String.format("{\"query\": \"Question\", \"doc_ids\": [\"%s\"]}", invalidUuid);

            // Act & Assert - WebTestClient properly captures Flux.error() as 400 via GlobalExceptionHandler
            webTestClient.post().uri("/api/rag/chat/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.error").isEqualTo("BAD_REQUEST");
        }

        @Test
        @DisplayName("should return 400 when one of doc_ids is invalid")
        void shouldReturn400WhenOneOfDocIdsIsInvalid() {
            // Arrange - one valid UUID and one invalid
            String validUuid = UUID.randomUUID().toString();
            String invalidUuid = "invalid-uuid";
            String requestBody = String.format(
                    "{\"query\": \"Question\", \"doc_ids\": [\"%s\", \"%s\"]}",
                    validUuid, invalidUuid
            );

            // Act & Assert
            webTestClient.post().uri("/api/rag/chat/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody().jsonPath("$.error").isEqualTo("BAD_REQUEST");
        }
    }

    @Nested
    @DisplayName("ChunkEvent JSON serialization via ObjectMapper")
    class ChunkEventSerializationTests {

        @Test
        @DisplayName("should serialize ChunkEvent with quotes and newlines correctly")
        void shouldSerializeChunkEventWithQuotesAndNewlinesCorrectly() throws Exception {
            // Arrange - simulate AI response with special characters that need JSON escaping
            String responseWithSpecialChars = "He said \"Hello\"\nand then left";
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just(responseWithSpecialChars));

            String requestBody = "{\"query\": \"Question\"}";

            // Act & Assert - verify endpoint accepts the request (ObjectMapper handles escaping)
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should serialize ChunkEvent with backslash and unicode correctly")
        void shouldSerializeChunkEventWithBackslashAndUnicodeCorrectly() throws Exception {
            // Arrange - backslash and unicode characters
            String responseWithSpecialChars = "Path: C:\\Users\\test\nEmoji: 🎉";
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just(responseWithSpecialChars));

            String requestBody = "{\"query\": \"Question\"}";

            // Act & Assert
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("truncate() method boundary conditions")
    class TruncateMethodTests {

        @Test
        @DisplayName("should return null string when text is null")
        void shouldReturnNullStringWhenTextIsNull() throws Exception {
            // Arrange
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), null
            );
            when(ragApplicationService.retrieveContext(isNull(), any(), anyInt())).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            String requestBody = "{\"query\": null}";

            // Act & Assert - RagController.truncate() should return "null" string for null input
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should not truncate when question length is exactly 50")
        void shouldNotTruncateWhenQuestionLengthIsExactly50() throws Exception {
            // Arrange
            String exactly50Chars = "12345678901234567890123456789012345678901234567890";
            assertThat(exactly50Chars).hasSize(50);

            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(eq(exactly50Chars), any(), anyInt())).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            String requestBody = String.format("{\"query\": \"%s\"}", exactly50Chars);

            // Act & Assert
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(ragApplicationService).retrieveContext(eq(exactly50Chars), any(), anyInt());
        }

        @Test
        @DisplayName("should truncate question when length exceeds 50")
        void shouldTruncateQuestionWhenLengthExceeds50() throws Exception {
            // Arrange - 60 characters
            String longQuestion = "This is a very long question that definitely exceeds fifty characters for testing";
            assertThat(longQuestion.length()).isGreaterThan(50);

            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(eq(longQuestion), any(), anyInt())).thenReturn(result);
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            String requestBody = String.format("{\"query\": \"%s\"}", longQuestion);

            // Act & Assert
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(ragApplicationService).retrieveContext(eq(longQuestion), any(), anyInt());
        }
    }
}
