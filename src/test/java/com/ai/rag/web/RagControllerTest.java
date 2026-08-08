package com.ai.rag.web;

import com.ai.account.web.OwnerContext;
import com.ai.common.domain.vo.OwnerKey;

import com.ai.rag.application.usecase.DocumentUploadService;
import com.ai.rag.application.usecase.RagApplicationService;
import com.ai.rag.application.usecase.RagChatUseCase;
import com.ai.rag.application.usecase.VisionChatUseCase;
import com.ai.rag.web.dto.RagChatRequest;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.vo.DocumentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagController")
class RagControllerTest {
    private OwnerContext ownerContext;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    @Mock
    private RagApplicationService ragApplicationService;

    @Mock
    private RagChatUseCase ragChatUseCase;

    @Mock
    private VisionChatUseCase visionChatUseCase;

    @Mock
    private ObjectProvider<VisionChatUseCase> visionChatUseCaseProvider;

    private ObjectMapper objectMapper;
    private RagController controller;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ownerContext = mock(OwnerContext.class);
        lenient().when(ownerContext.requireValue(any())).thenReturn("c:test-owner");
        lenient().when(visionChatUseCaseProvider.getIfAvailable()).thenReturn(visionChatUseCase);
        controller = new RagController(
                ragApplicationService, ragChatUseCase, visionChatUseCaseProvider, ownerContext);
    }

    @Nested
    @DisplayName("GET /api/rag/documents")
    class ListDocuments {

        @Test
        @DisplayName("should return list of documents")
        void shouldReturnListOfDocuments() {
            Document doc = createTestDocument("Test Doc", DocumentStatus.READY);
            when(ragApplicationService.listDocuments("c:test-owner")).thenReturn(List.of(doc));

            ResponseEntity<?> response = controller.listDocuments(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(ragApplicationService).listDocuments("c:test-owner");
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            when(ragApplicationService.listDocuments("c:test-owner")).thenReturn(List.of());

            ResponseEntity<?> response = controller.listDocuments(request);

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

            DocumentUploadService.UploadResult uploadResult =
                    new DocumentUploadService.UploadResult(doc.getId(), "test.txt", "READY", 0);
            when(ragApplicationService.uploadDocument(any(MultipartFile.class), isNull(), eq("c:test-owner")))
                    .thenReturn(uploadResult);

            ResponseEntity<?> response = controller.uploadDocument(file, null, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(ragApplicationService).uploadDocument(any(MultipartFile.class), isNull(), eq("c:test-owner"));
        }

        @Test
        @DisplayName("should use custom title when provided")
        void shouldUseCustomTitleWhenProvided() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "original.txt", "text/plain", "Content".getBytes());
            Document doc = createTestDocument("Custom Title", DocumentStatus.READY);

            DocumentUploadService.UploadResult uploadResult =
                    new DocumentUploadService.UploadResult(doc.getId(), "Custom Title", "READY", 0);
            when(ragApplicationService.uploadDocument(any(MultipartFile.class), eq("Custom Title"), eq("c:test-owner")))
                    .thenReturn(uploadResult);

            ResponseEntity<?> response = controller.uploadDocument(file, "Custom Title", request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            verify(ragApplicationService).uploadDocument(any(MultipartFile.class), eq("Custom Title"), eq("c:test-owner"));
        }

        @Test
        @DisplayName("should throw exception when upload fails")
        void shouldThrowExceptionWhenUploadFails() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "PDF content".getBytes());

            when(ragApplicationService.uploadDocument(any(MultipartFile.class), isNull(), eq("c:test-owner")))
                    .thenThrow(new RuntimeException("Upload failed"));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                controller.uploadDocument(file, null, request);
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
            doNothing().when(ragApplicationService).deleteDocument(docId, "c:test-owner");

            ResponseEntity<?> response = controller.deleteDocument(docId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(ragApplicationService).deleteDocument(docId, "c:test-owner");
        }
    }

    @Nested
    @DisplayName("POST /api/rag/chat/stream")
    class RagChatStream {

        @Test
        @DisplayName("should handle RAG chat request")
        void shouldHandleRagChatRequest() {
            RagChatRequest request = new RagChatRequest("What is AI?", null, null, 0.7, null, null);
            when(ragChatUseCase.chatStream(eq("What is AI?"), isNull(), eq(5), isNull()))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("AI response ").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response.collectList().block()).isNotEmpty();
            verify(ragChatUseCase).chatStream(eq("What is AI?"), isNull(), eq(5), isNull());
            verifyNoInteractions(visionChatUseCase);
        }

        @Test
        @DisplayName("should use docIds when provided")
        void shouldUseDocIdsWhenProvided() {
            List<String> docIds = List.of(UUID.randomUUID().toString());
            RagChatRequest request = new RagChatRequest("Question", null, null, 0.7, docIds, null);
            when(ragChatUseCase.chatStream(eq("Question"), eq(docIds), eq(5), isNull()))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response ").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response.collectList().block()).hasSize(1);
            verify(ragChatUseCase).chatStream(eq("Question"), eq(docIds), eq(5), isNull());
        }

        @Test
        @DisplayName("should use custom topK when provided")
        void shouldUseCustomTopKWhenProvided() {
            RagChatRequest request = new RagChatRequest("Question", null, 10, 0.7, null, null);
            when(ragChatUseCase.chatStream(eq("Question"), isNull(), eq(10), isNull()))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response ").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response.collectList().block()).hasSize(1);
            verify(ragChatUseCase).chatStream(eq("Question"), isNull(), eq(10), isNull());
        }

        @Test
        @DisplayName("should stream vision RAG when images provided")
        void should_streamVisionRagWhenImagesProvided() {
            List<String> images = List.of("iVBORw0KGgo=");
            RagChatRequest request = new RagChatRequest("Describe image", null, null, 0.7, null, images);
            when(visionChatUseCase.chatStreamWithImages(
                    eq("Describe image"), isNull(), eq(images), eq(5)))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("token").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response.collectList().block()).hasSize(1);
            verify(visionChatUseCase).chatStreamWithImages(
                    eq("Describe image"), isNull(), eq(images), eq(5));
            verify(ragChatUseCase, never()).chatStream(anyString(), any(), any(), any());
        }

        @Test
        @DisplayName("should fallback to text stream when vision unavailable")
        void should_fallbackToTextStreamWhenVisionUnavailable() {
            when(visionChatUseCaseProvider.getIfAvailable()).thenReturn(null);
            List<String> images = List.of("iVBORw0KGgo=");
            RagChatRequest request = new RagChatRequest("Describe image", null, null, 0.7, null, images);
            when(ragChatUseCase.chatStream(eq("Describe image"), isNull(), eq(5), isNull()))
                    .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("text ").build()));

            Flux<ServerSentEvent<String>> response = controller.ragChatStream(request);

            assertThat(response.collectList().block()).hasSize(1);
            verify(ragChatUseCase).chatStream(eq("Describe image"), isNull(), eq(5), isNull());
            verifyNoInteractions(visionChatUseCase);
        }

        @Test
        @DisplayName("should propagate exception when service fails")
        void shouldPropagateExceptionWhenServiceFails() {
            RagChatRequest request = new RagChatRequest("Question", null, null, 0.7, null, null);
            when(ragChatUseCase.chatStream(anyString(), any(), any(), any()))
                    .thenReturn(Flux.error(new RuntimeException("Service error")));

            assertThatThrownBy(() -> controller.ragChatStream(request).blockLast())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Service error");
        }
    }

    private Document createTestDocument(String title, DocumentStatus status) {
        Document doc = new Document(DocumentId.generate(), title, title, 1024L);
        return doc;
    }
}
