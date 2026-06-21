package com.ai.modules.rag.web;

import com.ai.modules.ai.infrastructure.streaming.StreamingService;
import com.ai.modules.rag.application.usecase.RagApplicationService;
import com.ai.modules.rag.application.usecase.DocumentUploadUseCase;
import com.ai.modules.rag.application.usecase.RagChatUseCase;
import com.ai.modules.rag.web.RagController;
import com.ai.modules.rag.web.dto.RagChatRequest;
import com.ai.modules.rag.domain.model.Document;
import com.ai.modules.rag.domain.model.DocumentStatus;
import com.ai.modules.rag.domain.model.SourceDocument;
import com.ai.modules.rag.domain.vo.DocumentId;
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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagController")
class RagControllerTest {

    @Mock
    private RagApplicationService ragApplicationService;

    @Mock
    private DocumentUploadUseCase documentUploadUseCase;

    @Mock
    private RagChatUseCase ragChatUseCase;

    @Mock
    private StreamingService streamingService;

    private ObjectMapper objectMapper;
    private RagController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new RagController(
                ragApplicationService, documentUploadUseCase, ragChatUseCase, streamingService);
    }

    @Nested
    @DisplayName("GET /api/rag/documents/")
    class ListDocuments {

        @Test
        @DisplayName("should return list of documents")
        void shouldReturnListOfDocuments() {
            Document doc = createTestDocument("Test Doc", DocumentStatus.READY);
            when(ragApplicationService.listDocuments()).thenReturn(List.of(doc));

            ResponseEntity<?> response = controller.listDocuments();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(ragApplicationService).listDocuments();
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            when(ragApplicationService.listDocuments()).thenReturn(List.of());

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

            DocumentUploadUseCase.UploadResult uploadResult =
                    new DocumentUploadUseCase.UploadResult(doc.getId(), "test.txt", "READY", 0);
            when(documentUploadUseCase.upload(any(MultipartFile.class), isNull()))
                    .thenReturn(uploadResult);

            ResponseEntity<?> response = controller.uploadDocument(file, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(documentUploadUseCase).upload(any(MultipartFile.class), isNull());
        }

        @Test
        @DisplayName("should use custom title when provided")
        void shouldUseCustomTitleWhenProvided() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "original.txt", "text/plain", "Content".getBytes());
            Document doc = createTestDocument("Custom Title", DocumentStatus.READY);

            DocumentUploadUseCase.UploadResult uploadResult =
                    new DocumentUploadUseCase.UploadResult(doc.getId(), "Custom Title", "READY", 0);
            when(documentUploadUseCase.upload(any(MultipartFile.class), eq("Custom Title")))
                    .thenReturn(uploadResult);

            ResponseEntity<?> response = controller.uploadDocument(file, "Custom Title");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(documentUploadUseCase).upload(any(MultipartFile.class), eq("Custom Title"));
        }

        @Test
        @DisplayName("should throw exception when upload fails")
        void shouldThrowExceptionWhenUploadFails() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "PDF content".getBytes());

            when(documentUploadUseCase.upload(any(MultipartFile.class), isNull()))
                    .thenThrow(new RuntimeException("Upload failed"));

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
            doNothing().when(ragApplicationService).deleteDocument(docId);

            ResponseEntity<?> response = controller.deleteDocument(docId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(ragApplicationService).deleteDocument(docId);
        }
    }

    @Nested
    @DisplayName("POST /api/rag/chat/stream")
    class RagChatStream {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should handle RAG chat request")
        void shouldHandleRagChatRequest() {
            RagChatRequest request = new RagChatRequest("What is AI?", null, null, 0.7, null);
            RagChatUseCase.ChatResult chatResult = new RagChatUseCase.ChatResult(
                    "AI response", List.of()
            );
            when(ragChatUseCase.chat(eq("What is AI?"), isNull(), eq(5))).thenReturn(chatResult);
            when(streamingService.streamWithSources(eq("AI response"), anyList()))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("AI response").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response).isNotNull();
            verify(ragChatUseCase).chat(eq("What is AI?"), isNull(), eq(5));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should use docIds when provided")
        void shouldUseDocIdsWhenProvided() {
            List<String> docIds = List.of(UUID.randomUUID().toString());
            RagChatRequest request = new RagChatRequest("Question", null, null, 0.7, docIds);
            RagChatUseCase.ChatResult chatResult = new RagChatUseCase.ChatResult(
                    "Response", List.of(new SourceDocument("doc", 0.9, null))
            );
            when(ragChatUseCase.chat(anyString(), any(), anyInt())).thenReturn(chatResult);
            when(streamingService.streamWithSources(eq("Response"), anyList()))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response).isNotNull();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should use custom topK when provided")
        void shouldUseCustomTopKWhenProvided() {
            RagChatRequest request = new RagChatRequest("Question", null, 10, 0.7, null);
            RagChatUseCase.ChatResult chatResult = new RagChatUseCase.ChatResult(
                    "Response", List.of()
            );
            when(ragChatUseCase.chat(anyString(), isNull(), eq(10))).thenReturn(chatResult);
            when(streamingService.streamWithSources(eq("Response"), anyList()))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response).isNotNull();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return error flux when exception occurs")
        void shouldReturnErrorFluxWhenExceptionOccurs() {
            RagChatRequest request = new RagChatRequest("Question", null, null, 0.7, null);
            when(ragChatUseCase.chat(anyString(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Service error"));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response).isNotNull();
        }
    }

    private Document createTestDocument(String title, DocumentStatus status) {
        Document doc = new Document(DocumentId.generate(), title, title, 1024L);
        return doc;
    }
}
