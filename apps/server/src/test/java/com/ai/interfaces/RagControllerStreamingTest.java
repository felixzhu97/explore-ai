package com.ai.interfaces;

import com.ai.application.service.LanguageDetectionService;
import com.ai.application.service.RagApplicationService;
import com.ai.application.usecase.RagChatUseCase;
import com.ai.domain.model.SourceDocument;
import com.ai.service.AiChatService;
import com.ai.domain.exception.RagServiceException;
import com.ai.infrastructure.adapter.document.PdfTextExtractor;
import com.ai.interfaces.controller.GlobalExceptionHandler;
import com.ai.interfaces.controller.RagController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RagController Streaming Tests
 *
 * Tests for streaming RAG endpoints using MockMvc.
 * Note: Full streaming response testing requires WebTestClient for reactive streams.
 */
@WebMvcTest(RagController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("RagController Streaming Tests")
class RagControllerStreamingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagApplicationService ragApplicationService;

    @MockBean
    private LanguageDetectionService languageDetectionService;

    @MockBean
    private AiChatService aiChatService;

    @MockBean
    private PdfTextExtractor pdfTextExtractor;

    @Nested
    @DisplayName("POST /api/rag/chat/stream endpoint")
    class StreamingEndpointTests {

        @Test
        @DisplayName("should call services for streaming RAG chat request")
        void shouldCallServicesForStreamingRagChatRequest() throws Exception {
            // Arrange
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                    "Context from documents",
                    List.of(new SourceDocument("Document 1", 0.9, null)),
                    "Enriched query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(languageDetectionService.detect(anyString())).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString())).thenReturn("Built prompt");

            String requestBody = "{\"query\": \"What is AI?\"}";

            // Act - verify the endpoint responds (full streaming tested via integration tests)
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            // Verify mock was called
            verify(ragApplicationService).retrieveContext(anyString(), any(), anyInt());
            verify(languageDetectionService).detect(anyString());
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
            when(aiChatService.chat(anyString())).thenReturn("Response");

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
            when(aiChatService.chat(anyString())).thenReturn("Response");

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
            when(aiChatService.chat(anyString())).thenReturn("Response");

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
            when(aiChatService.chat(anyString())).thenReturn("Response");

            String requestBody = "{\"query\": \"Question\", \"doc_ids\": null}";

            // Act
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(ragApplicationService).retrieveContext(eq("Question"), isNull(), anyInt());
        }
    }
}
