package com.ai.adapter.in.controller;

import com.ai.adapter.out.document.PdfTextExtractor;
import com.ai.adapter.in.dto.RagChatRequest;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentStatus;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.AiChatService;
import com.ai.domain.service.LanguageDetectionService;
import com.ai.domain.service.PromptTemplates;
import com.ai.domain.service.RagService;
import com.ai.domain.vo.DocumentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagController")
class RagControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private LanguageDetectionService languageDetectionService;

    @Mock
    private PromptTemplates promptTemplates;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    private ObjectMapper objectMapper;
    private RagController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new RagController(
                ragService, languageDetectionService, promptTemplates, aiChatService, objectMapper, pdfTextExtractor);
    }

    @Nested
    @DisplayName("GET /api/rag/documents/")
    class ListDocuments {

        @Test
        @DisplayName("should return list of documents")
        void shouldReturnListOfDocuments() {
            Document doc = createTestDocument("Test Doc", DocumentStatus.READY);
            when(ragService.listDocuments()).thenReturn(List.of(doc));

            ResponseEntity<?> response = controller.listDocuments();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(ragService).listDocuments();
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            when(ragService.listDocuments()).thenReturn(List.of());

            ResponseEntity<?> response = controller.listDocuments();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("POST /api/rag/documents/upload")
    class UploadDocument {

        @Test
        @DisplayName("should upload text file successfully")
        void shouldUploadTextFileSuccessfully() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "Hello World".getBytes());
            Document doc = createTestDocument("test.txt", DocumentStatus.READY);
            when(ragService.uploadDocument(eq("test.txt"), eq("test.txt"), anyLong(), anyString()))
                    .thenReturn(doc);

            ResponseEntity<?> response = controller.uploadDocument(file, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(ragService).uploadDocument(eq("test.txt"), eq("test.txt"), anyLong(), eq("Hello World"));
        }

        @Test
        @DisplayName("should use custom title when provided")
        void shouldUseCustomTitleWhenProvided() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "original.txt", "text/plain", "Content".getBytes());
            Document doc = createTestDocument("Custom Title", DocumentStatus.READY);
            when(ragService.uploadDocument(eq("Custom Title"), eq("original.txt"), anyLong(), anyString()))
                    .thenReturn(doc);

            ResponseEntity<?> response = controller.uploadDocument(file, "Custom Title");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        @Test
        @DisplayName("should extract text from PDF")
        void shouldExtractTextFromPdf() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "PDF content".getBytes());
            Document doc = createTestDocument("document.pdf", DocumentStatus.READY);
            when(pdfTextExtractor.getExtension("document.pdf")).thenReturn("pdf");
            when(pdfTextExtractor.extractText(any())).thenReturn(Optional.of("Extracted PDF text"));
            when(ragService.uploadDocument(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(doc);

            ResponseEntity<?> response = controller.uploadDocument(file, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(pdfTextExtractor).extractText(any());
        }

        @Test
        @DisplayName("should throw exception when PDF extraction fails")
        void shouldThrowExceptionWhenPdfExtractionFails() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "PDF content".getBytes());
            when(pdfTextExtractor.getExtension("document.pdf")).thenReturn("pdf");
            when(pdfTextExtractor.extractText(any())).thenReturn(Optional.empty());

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                controller.uploadDocument(file, null);
            });
        }
    }

    @Nested
    @DisplayName("DELETE /api/rag/documents/{id}")
    class DeleteDocument {

        @Test
        @DisplayName("should delete document and return 204")
        void shouldDeleteDocumentAndReturn204() {
            UUID docId = UUID.randomUUID();
            doNothing().when(ragService).deleteDocument(docId);

            ResponseEntity<?> response = controller.deleteDocument(docId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(ragService).deleteDocument(docId);
        }
    }

    @Nested
    @DisplayName("POST /api/rag/chat/stream")
    class RagChatStream {

        @Test
        @DisplayName("should handle RAG chat request")
        void shouldHandleRagChatRequest() {
            RagChatRequest request = new RagChatRequest("What is AI?", null, null, 0.7, null);
            RagService.RetrievalResult result = new RagService.RetrievalResult(
                    "Context from docs", List.of(), "Enriched query"
            );
            when(ragService.retrieveContext(eq("What is AI?"), isNull(), eq(5))).thenReturn(result);
            when(languageDetectionService.detect("What is AI?")).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString()))
                    .thenReturn("Built prompt");
            when(aiChatService.chat(anyString())).thenReturn("AI response");

            var response = controller.ragChatStream(request);

            assertThat(response).isNotNull();
            verify(ragService).retrieveContext(eq("What is AI?"), isNull(), eq(5));
        }

        @Test
        @DisplayName("should use docIds when provided")
        void shouldUseDocIdsWhenProvided() {
            List<String> docIds = List.of(UUID.randomUUID().toString());
            RagChatRequest request = new RagChatRequest("Question", null, null, 0.7, docIds);
            RagService.RetrievalResult result = new RagService.RetrievalResult(
                    "Context", List.of(new SourceDocument("doc", 0.9, null)), "Query"
            );
            when(ragService.retrieveContext(anyString(), any(), anyInt())).thenReturn(result);
            when(languageDetectionService.detect(anyString())).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString()))
                    .thenReturn("Prompt");
            when(aiChatService.chat(anyString())).thenReturn("Response");

            var response = controller.ragChatStream(request);

            assertThat(response).isNotNull();
            verify(ragService).retrieveContext(eq("Question"), any(), eq(5));
        }

        @Test
        @DisplayName("should use custom topK when provided")
        void shouldUseCustomTopKWhenProvided() {
            RagChatRequest request = new RagChatRequest("Question", null, 10, 0.7, null);
            RagService.RetrievalResult result = new RagService.RetrievalResult(
                    "Context", List.of(), "Query"
            );
            when(ragService.retrieveContext(anyString(), isNull(), eq(10))).thenReturn(result);
            when(languageDetectionService.detect(anyString())).thenReturn("en");
            when(languageDetectionService.buildPrompt(anyString(), anyString(), anyString()))
                    .thenReturn("Prompt");
            when(aiChatService.chat(anyString())).thenReturn("Response");

            var response = controller.ragChatStream(request);

            assertThat(response).isNotNull();
            verify(ragService).retrieveContext(anyString(), isNull(), eq(10));
        }
    }

    private Document createTestDocument(String title, DocumentStatus status) {
        Document doc = new Document(DocumentId.generate(), title, title, 1024L);
        return doc;
    }
}
