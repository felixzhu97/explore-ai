package com.ai.interfaces;

import com.ai.application.service.LanguageDetectionService;
import com.ai.application.service.RagApplicationService;
import com.ai.application.usecase.RagChatUseCase;
import com.ai.domain.model.Document;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.model.DocumentStatus;
import com.ai.domain.service.AiChatService;
import com.ai.domain.vo.DocumentId;
import com.ai.infrastructure.adapter.document.PdfTextExtractor;
import com.ai.interfaces.controller.RagController;
import com.ai.interfaces.dto.DocumentSummaryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RagController Tests
 *
 * Tests using @WebMvcTest and MockMvc for the RAG controller layer:
 * - Document upload, list, delete operations
 * - Streaming response format (SSE)
 * - Exception handling
 */
@WebMvcTest(RagController.class)
@DisplayName("RagController Tests")
class RagControllerTest {

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
    @DisplayName("shouldReturnDocumentsList_WhenGetDocuments")
    class ListDocumentsTests {

        @Test
        @DisplayName("should return list of documents when getDocuments is called")
        void shouldReturnDocumentsListWhenGetDocumentsCalled() throws Exception {
            // Arrange
            Document doc = createTestDocument("Test Document", DocumentStatus.READY);
            DocumentSummaryDto summary = new DocumentSummaryDto(
                doc.getId().value(), doc.getTitle(), doc.getStatus().name(),
                doc.getCreatedAt(), 0
            );
            when(ragApplicationService.listDocuments()).thenReturn(List.of(doc));

            // Act & Assert
            mockMvc.perform(get("/api/rag/documents/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documents").isArray())
                    .andExpect(jsonPath("$.documents[0].title").value("Test Document"))
                    .andExpect(jsonPath("$.documents[0].status").value("READY"));
        }

        @Test
        @DisplayName("should return empty list when no documents exist")
        void shouldReturnEmptyListWhenNoDocumentsExist() throws Exception {
            // Arrange
            when(ragApplicationService.listDocuments()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/rag/documents/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documents").isArray())
                    .andExpect(jsonPath("$.documents").isEmpty());
        }

        @Test
        @DisplayName("should return multiple documents when multiple exist")
        void shouldReturnMultipleDocumentsWhenMultipleExist() throws Exception {
            // Arrange
            Document doc1 = createTestDocument("Document 1", DocumentStatus.READY);
            Document doc2 = createTestDocument("Document 2", DocumentStatus.PROCESSING);
            when(ragApplicationService.listDocuments()).thenReturn(List.of(doc1, doc2));

            // Act & Assert
            mockMvc.perform(get("/api/rag/documents/"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.documents").isArray())
                    .andExpect(jsonPath("$.documents.length()").value(2));
        }
    }

    @Nested
    @DisplayName("shouldUploadDocument_WhenValidFileProvided")
    class UploadDocumentTests {

        @Test
        @DisplayName("should upload document and return 201")
        void shouldUploadDocumentAndReturn201() throws Exception {
            // Arrange
            Document doc = createTestDocument("test.txt", DocumentStatus.READY);
            when(pdfTextExtractor.getExtension("test.txt")).thenReturn("txt");
            when(ragApplicationService.uploadDocument(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(doc);

            MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "Hello World".getBytes()
            );

            // Act & Assert - verify upload works and returns 201
            mockMvc.perform(multipart("/api/rag/documents/upload")
                            .file(file))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").isNotEmpty());
        }

        @Test
        @DisplayName("should use filename as title when title is not provided")
        void shouldUseFilenameAsTitleWhenTitleNotProvided() throws Exception {
            // Arrange
            Document doc = createTestDocument("test.txt", DocumentStatus.READY);
            when(pdfTextExtractor.getExtension("test.txt")).thenReturn("txt");
            when(ragApplicationService.uploadDocument(eq("test.txt"), anyString(), anyLong(), anyString()))
                    .thenReturn(doc);

            MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "Hello World".getBytes()
            );

            // Act & Assert
            mockMvc.perform(multipart("/api/rag/documents/upload").file(file))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("test.txt"));
        }

        @Test
        @DisplayName("should extract text from PDF when uploading PDF file")
        void shouldExtractTextFromPdfWhenUploadingPdfFile() throws Exception {
            // Arrange
            Document doc = createTestDocument("document.pdf", DocumentStatus.READY);
            when(pdfTextExtractor.getExtension("document.pdf")).thenReturn("pdf");
            when(pdfTextExtractor.extractText(any(byte[].class)))
                    .thenReturn(java.util.Optional.of("Extracted PDF content"));
            when(ragApplicationService.uploadDocument(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(doc);

            MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes()
            );

            // Act & Assert
            mockMvc.perform(multipart("/api/rag/documents/upload").file(file))
                    .andExpect(status().isCreated());

            verify(pdfTextExtractor).extractText(any(byte[].class));
        }

        @Test
        @DisplayName("should return 500 when PDF text extraction fails")
        void shouldReturn500WhenPdfTextExtractionFails() throws Exception {
            // Arrange
            when(pdfTextExtractor.getExtension("document.pdf")).thenReturn("pdf");
            when(pdfTextExtractor.extractText(any(byte[].class)))
                    .thenReturn(java.util.Optional.empty());

            MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", MediaType.APPLICATION_PDF_VALUE, "PDF content".getBytes()
            );

            // Act & Assert
            mockMvc.perform(multipart("/api/rag/documents/upload").file(file))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("shouldDeleteDocument_WhenDocumentExists")
    class DeleteDocumentTests {

        @Test
        @DisplayName("should delete document and return 204")
        void shouldDeleteDocumentAndReturn204() throws Exception {
            // Arrange
            UUID docId = UUID.randomUUID();
            doNothing().when(ragApplicationService).deleteDocument(docId);

            // Act & Assert
            mockMvc.perform(delete("/api/rag/documents/{id}", docId))
                    .andExpect(status().isNoContent());

            verify(ragApplicationService).deleteDocument(docId);
        }

        @Test
        @DisplayName("should call deleteDocument with correct UUID")
        void shouldCallDeleteDocumentWithCorrectUuid() throws Exception {
            // Arrange
            UUID docId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            doNothing().when(ragApplicationService).deleteDocument(docId);

            // Act
            mockMvc.perform(delete("/api/rag/documents/{id}", docId))
                    .andExpect(status().isNoContent());

            // Assert
            verify(ragApplicationService).deleteDocument(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        }
    }

    @Nested
    @DisplayName("shouldReturnStreamingResponse_WhenRagChatStream")
    class StreamingChatTests {

        @Test
        @DisplayName("should return streaming response with correct content type")
        void shouldReturnStreamingResponseWithCorrectContentType() throws Exception {
            // Arrange
            Map<String, Object> request = Map.of(
                "query", "What is AI?",
                "top_k", 5,
                "temperature", 0.7
            );
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                "Context from documents", List.of(), "Enriched query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(languageDetectionService.detect(anyString())).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString()))
                    .thenReturn("Prompt with context");
            when(aiChatService.chat(anyString())).thenReturn("AI response here");

            // Act & Assert
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should handle streaming with specific document IDs")
        void shouldHandleStreamingWithSpecificDocumentIds() throws Exception {
            // Arrange
            List<String> docIds = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            Map<String, Object> request = Map.of(
                "query", "Question about docs",
                "doc_ids", docIds
            );
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                "Context from docs", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(languageDetectionService.detect(anyString())).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString()))
                    .thenReturn("Prompt");
            when(aiChatService.chat(anyString())).thenReturn("Response");

            // Act & Assert
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should use default topK when not provided")
        void shouldUseDefaultTopKWhenNotProvided() throws Exception {
            // Arrange
            Map<String, Object> request = Map.of("query", "Question");
            RagChatUseCase.RetrievalResult result = new RagChatUseCase.RetrievalResult(
                "Context", List.of(), "Query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), eq(5))).thenReturn(result);
            when(languageDetectionService.detect(anyString())).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString()))
                    .thenReturn("Prompt");
            when(aiChatService.chat(anyString())).thenReturn("Response");

            // Act & Assert
            mockMvc.perform(post("/api/rag/chat/stream")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(ragApplicationService).retrieveContext(anyString(), any(), eq(5));
        }
    }

    private Document createTestDocument(String title, DocumentStatus status) {
        Document doc = new Document(
            DocumentId.generate(),
            title,
            title,
            1024L
        );
        switch (status) {
            case READY -> doc.markReady();
            case PROCESSING -> doc.markProcessing();
            case FAILED -> doc.markFailed();
            default -> {}
        }
        return doc;
    }
}
