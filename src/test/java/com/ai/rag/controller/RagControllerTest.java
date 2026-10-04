package com.ai.rag.controller;

import static com.ai.testsupport.MvcStreamTestSupport.STREAM_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.controller.GlobalExceptionHandler;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.service.DocumentUploadService;
import com.ai.rag.service.RagApplicationService;
import com.ai.rag.service.RagChatService;
import com.ai.testsupport.AbstractOwnerScopedControllerTest;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import com.ai.vision.service.VisionChatService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;

@SliceWebMvcTest(controllers = RagController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("RagController")
class RagControllerTest extends AbstractOwnerScopedControllerTest {

  @MockitoBean private RagApplicationService ragApplicationService;

  @MockitoBean private RagChatService ragChatService;

  @MockitoBean private VisionChatService visionChatService;

  @MockitoBean private ObjectProvider<VisionChatService> visionChatServiceProvider;

  @BeforeEach
  void setUpVisionProvider() {
    lenient().when(visionChatServiceProvider.getIfAvailable()).thenReturn(visionChatService);
  }

  @Nested
  @DisplayName("GET /api/rag/documents")
  class ListDocuments {

    @Test
    @DisplayName("should return list of documents")
    void shouldReturnListOfDocuments() {
      RagDocument doc = createTestDocument("Test Doc", DocumentStatus.READY);
      when(ragApplicationService.listDocuments(ownerKey())).thenReturn(List.of(doc));

      assertThat(
              mvc.get()
                  .uri("/api/rag/documents")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.documents.length()")
          .convertTo(Integer.class)
          .isEqualTo(1);
      verify(ragApplicationService).listDocuments(ownerKey());
    }

    @Test
    @DisplayName("should return stored chunk count for each document")
    void shouldReturnStoredChunkCountForEachDocument() {
      RagDocument doc = createTestDocument("Chunked Doc", DocumentStatus.READY);
      when(ragApplicationService.listDocuments(ownerKey())).thenReturn(List.of(doc));
      when(ragApplicationService.chunkCounts(List.of(doc))).thenReturn(Map.of(doc.getId(), 17));

      assertThat(
              mvc.get()
                  .uri("/api/rag/documents")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.documents[0].chunkCount")
          .convertTo(Integer.class)
          .isEqualTo(17);
    }

    @Test
    @DisplayName("should return empty list when no documents")
    void shouldReturnEmptyListWhenNoDocuments() {
      when(ragApplicationService.listDocuments(ownerKey())).thenReturn(List.of());

      assertThat(
              mvc.get()
                  .uri("/api/rag/documents")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.documents.length()")
          .convertTo(Integer.class)
          .isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("POST /api/rag/documents/upload")
  class UploadDocument {

    @Test
    @DisplayName("should upload text file successfully")
    void shouldUploadTextFileSuccessfully() {
      MockMultipartFile file =
          new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());
      RagDocument doc = createTestDocument("test.txt", DocumentStatus.READY);
      DocumentUploadService.UploadResult uploadResult =
          new DocumentUploadService.UploadResult(doc.getId(), "test.txt", "READY", 0);
      when(ragApplicationService.uploadDocument(any(), isNull(), eq(ownerKey())))
          .thenReturn(uploadResult);

      assertThat(
              mvc.post()
                  .multipart()
                  .uri("/api/rag/documents/upload")
                  .file(file)
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatus(HttpStatus.CREATED);
      verify(ragApplicationService).uploadDocument(any(), isNull(), eq(ownerKey()));
    }

    @Test
    @DisplayName("should use custom title when provided")
    void shouldUseCustomTitleWhenProvided() {
      MockMultipartFile file =
          new MockMultipartFile("file", "original.txt", "text/plain", "Content".getBytes());
      RagDocument doc = createTestDocument("Custom Title", DocumentStatus.READY);
      DocumentUploadService.UploadResult uploadResult =
          new DocumentUploadService.UploadResult(doc.getId(), "Custom Title", "READY", 0);
      when(ragApplicationService.uploadDocument(any(), eq("Custom Title"), eq(ownerKey())))
          .thenReturn(uploadResult);

      assertThat(
              mvc.post()
                  .multipart()
                  .uri("/api/rag/documents/upload")
                  .file(file)
                  .param("title", "Custom Title")
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatus(HttpStatus.CREATED);
      verify(ragApplicationService).uploadDocument(any(), eq("Custom Title"), eq(ownerKey()));
    }

    @Test
    @DisplayName("should return 500 when upload fails")
    void shouldReturn500WhenUploadFails() {
      MockMultipartFile file =
          new MockMultipartFile(
              "file", "document.pdf", "application/pdf", "PDF content".getBytes());
      when(ragApplicationService.uploadDocument(any(), isNull(), eq(ownerKey())))
          .thenThrow(new RuntimeException("Upload failed"));

      assertThat(
              mvc.post()
                  .multipart()
                  .uri("/api/rag/documents/upload")
                  .file(file)
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Nested
  @DisplayName("DELETE /api/rag/documents/{id}")
  class DeleteDocument {

    @Test
    @DisplayName("should delete document and return 204")
    void shouldDeleteDocumentAndReturn204() {
      UUID docId = UUID.randomUUID();
      doNothing().when(ragApplicationService).deleteDocument(docId, ownerKey());

      assertThat(
              mvc.delete()
                  .uri("/api/rag/documents/" + docId)
                  .with(ClientIdentityRequestPostProcessor.withClientId(ownerKey())))
          .hasStatus(HttpStatus.NO_CONTENT);
      verify(ragApplicationService).deleteDocument(docId, ownerKey());
    }
  }

  @Nested
  @DisplayName("POST /api/rag/chat/stream")
  class RagChatStream {

    @Test
    @DisplayName("should handle RAG chat request")
    void shouldHandleRagChatRequest() {
      when(ragChatService.chatStream(eq("What is AI?"), isNull(), eq(5), isNull()))
          .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("AI response ").build()));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"What is AI?\"}")
                  .exchange(STREAM_TIMEOUT))
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("AI response");
      verify(ragChatService).chatStream(eq("What is AI?"), isNull(), eq(5), isNull());
      verifyNoInteractions(visionChatService);
    }

    @Test
    @DisplayName("should use documentIds when provided")
    void shouldUseDocIdsWhenProvided() {
      List<String> documentIds = List.of(UUID.randomUUID().toString());
      when(ragChatService.chatStream(eq("Question"), eq(documentIds), eq(5), isNull()))
          .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response ").build()));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "question": "Question",
                        "documentIds": ["%s"]
                      }
                      """
                          .formatted(documentIds.getFirst()))
                  .exchange(STREAM_TIMEOUT))
          .hasStatusOk();
      verify(ragChatService).chatStream(eq("Question"), eq(documentIds), eq(5), isNull());
    }

    @Test
    @DisplayName("should use custom topK when provided")
    void shouldUseCustomTopKWhenProvided() {
      when(ragChatService.chatStream(eq("Question"), isNull(), eq(10), isNull()))
          .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response ").build()));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"Question\",\"topK\":10}")
                  .exchange(STREAM_TIMEOUT))
          .hasStatusOk();
      verify(ragChatService).chatStream(eq("Question"), isNull(), eq(10), isNull());
    }

    @Test
    @DisplayName("should stream vision RAG when images provided")
    void shouldStreamVisionRagWhenImagesProvided() {
      List<String> images = List.of("iVBORw0KGgo=");
      when(visionChatService.chatStreamWithImages(
              eq("Describe image"), isNull(), eq(images), eq(5)))
          .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("token").build()));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "question": "Describe image",
                        "images": ["iVBORw0KGgo="]
                      }
                      """)
                  .exchange(STREAM_TIMEOUT))
          .hasStatusOk();
      verify(visionChatService)
          .chatStreamWithImages(eq("Describe image"), isNull(), eq(images), eq(5));
      verify(ragChatService, never()).chatStream(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should propagate stream error from service")
    void shouldPropagateStreamErrorFromService() {
      when(ragChatService.chatStream(any(), any(), any(), any()))
          .thenReturn(Flux.error(new RuntimeException("Service error")));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"Question\"}")
                  .exchange(STREAM_TIMEOUT))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      verify(ragChatService).chatStream(eq("Question"), isNull(), eq(5), isNull());
    }
  }

  private RagDocument createTestDocument(String title, DocumentStatus status) {
    return new RagDocument(DocumentId.generate(), title, title, 1024L, "c:test");
  }
}
