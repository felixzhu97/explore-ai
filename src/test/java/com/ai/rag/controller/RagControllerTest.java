package com.ai.rag.controller;

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

import com.ai.account.controller.OwnerContext;
import com.ai.common.controller.GlobalExceptionHandler;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.service.usecase.DocumentUploadService;
import com.ai.rag.service.usecase.RagApplicationService;
import com.ai.rag.service.usecase.RagChatUseCase;
import com.ai.rag.service.usecase.VisionChatUseCase;
import com.ai.testsupport.ClientIdentityRequestPostProcessor;
import com.ai.testsupport.SliceWebMvcTest;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import reactor.core.publisher.Flux;

@SliceWebMvcTest(controllers = RagController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("RagController")
class RagControllerTest {

  private static final String CLIENT_ID = "c:test-owner";

  @Autowired private MockMvcTester mvc;

  @MockitoBean private RagApplicationService ragApplicationService;

  @MockitoBean private RagChatUseCase ragChatUseCase;

  @MockitoBean private VisionChatUseCase visionChatUseCase;

  @MockitoBean private ObjectProvider<VisionChatUseCase> visionChatUseCaseProvider;

  @MockitoBean private OwnerContext ownerContext;

  @BeforeEach
  void setUp() {
    lenient().when(ownerContext.requireValue(any())).thenReturn(CLIENT_ID);
    lenient().when(visionChatUseCaseProvider.getIfAvailable()).thenReturn(visionChatUseCase);
  }

  @Nested
  @DisplayName("GET /api/rag/documents")
  class ListDocuments {

    @Test
    @DisplayName("should return list of documents")
    void shouldReturnListOfDocuments() {
      Document doc = createTestDocument("Test Doc", DocumentStatus.READY);
      when(ragApplicationService.listDocuments(CLIENT_ID)).thenReturn(List.of(doc));

      assertThat(
              mvc.get()
                  .uri("/api/rag/documents")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.documents.length()")
          .convertTo(Integer.class)
          .isEqualTo(1);
      verify(ragApplicationService).listDocuments(CLIENT_ID);
    }

    @Test
    @DisplayName("should return empty list when no documents")
    void shouldReturnEmptyListWhenNoDocuments() {
      when(ragApplicationService.listDocuments(CLIENT_ID)).thenReturn(List.of());

      assertThat(
              mvc.get()
                  .uri("/api/rag/documents")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
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
      Document doc = createTestDocument("test.txt", DocumentStatus.READY);
      DocumentUploadService.UploadResult uploadResult =
          new DocumentUploadService.UploadResult(doc.getId(), "test.txt", "READY", 0);
      when(ragApplicationService.uploadDocument(any(), isNull(), eq(CLIENT_ID)))
          .thenReturn(uploadResult);

      assertThat(
              mvc.post()
                  .multipart()
                  .uri("/api/rag/documents/upload")
                  .file(file)
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatus(HttpStatus.CREATED);
      verify(ragApplicationService).uploadDocument(any(), isNull(), eq(CLIENT_ID));
    }

    @Test
    @DisplayName("should use custom title when provided")
    void shouldUseCustomTitleWhenProvided() {
      MockMultipartFile file =
          new MockMultipartFile("file", "original.txt", "text/plain", "Content".getBytes());
      Document doc = createTestDocument("Custom Title", DocumentStatus.READY);
      DocumentUploadService.UploadResult uploadResult =
          new DocumentUploadService.UploadResult(doc.getId(), "Custom Title", "READY", 0);
      when(ragApplicationService.uploadDocument(any(), eq("Custom Title"), eq(CLIENT_ID)))
          .thenReturn(uploadResult);

      assertThat(
              mvc.post()
                  .multipart()
                  .uri("/api/rag/documents/upload")
                  .file(file)
                  .param("title", "Custom Title")
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatus(HttpStatus.CREATED);
      verify(ragApplicationService).uploadDocument(any(), eq("Custom Title"), eq(CLIENT_ID));
    }

    @Test
    @DisplayName("should return 500 when upload fails")
    void shouldReturn500WhenUploadFails() {
      MockMultipartFile file =
          new MockMultipartFile(
              "file", "document.pdf", "application/pdf", "PDF content".getBytes());
      when(ragApplicationService.uploadDocument(any(), isNull(), eq(CLIENT_ID)))
          .thenThrow(new RuntimeException("Upload failed"));

      assertThat(
              mvc.post()
                  .multipart()
                  .uri("/api/rag/documents/upload")
                  .file(file)
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
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
      doNothing().when(ragApplicationService).deleteDocument(docId, CLIENT_ID);

      assertThat(
              mvc.delete()
                  .uri("/api/rag/documents/" + docId)
                  .with(ClientIdentityRequestPostProcessor.withClientId(CLIENT_ID)))
          .hasStatus(HttpStatus.NO_CONTENT);
      verify(ragApplicationService).deleteDocument(docId, CLIENT_ID);
    }
  }

  @Nested
  @DisplayName("POST /api/rag/chat/stream")
  class RagChatStream {

    @Test
    @DisplayName("should handle RAG chat request")
    void shouldHandleRagChatRequest() {
      when(ragChatUseCase.chatStream(eq("What is AI?"), isNull(), eq(5), isNull()))
          .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("AI response ").build()));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"What is AI?\"}")
                  .exchange(Duration.ofSeconds(5)))
          .hasStatusOk()
          .bodyText()
          .asString()
          .contains("AI response");
      verify(ragChatUseCase).chatStream(eq("What is AI?"), isNull(), eq(5), isNull());
      verifyNoInteractions(visionChatUseCase);
    }

    @Test
    @DisplayName("should use docIds when provided")
    void shouldUseDocIdsWhenProvided() {
      List<String> docIds = List.of(UUID.randomUUID().toString());
      when(ragChatUseCase.chatStream(eq("Question"), eq(docIds), eq(5), isNull()))
          .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response ").build()));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {
                        "question": "Question",
                        "doc_ids": ["%s"]
                      }
                      """
                          .formatted(docIds.getFirst()))
                  .exchange(Duration.ofSeconds(5)))
          .hasStatusOk();
      verify(ragChatUseCase).chatStream(eq("Question"), eq(docIds), eq(5), isNull());
    }

    @Test
    @DisplayName("should use custom topK when provided")
    void shouldUseCustomTopKWhenProvided() {
      when(ragChatUseCase.chatStream(eq("Question"), isNull(), eq(10), isNull()))
          .thenReturn(Flux.just(ServerSentEvent.<String>builder().data("Response ").build()));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"Question\",\"top_k\":10}")
                  .exchange(Duration.ofSeconds(5)))
          .hasStatusOk();
      verify(ragChatUseCase).chatStream(eq("Question"), isNull(), eq(10), isNull());
    }

    @Test
    @DisplayName("should stream vision RAG when images provided")
    void shouldStreamVisionRagWhenImagesProvided() {
      List<String> images = List.of("iVBORw0KGgo=");
      when(visionChatUseCase.chatStreamWithImages(
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
                  .exchange(Duration.ofSeconds(5)))
          .hasStatusOk();
      verify(visionChatUseCase)
          .chatStreamWithImages(eq("Describe image"), isNull(), eq(images), eq(5));
      verify(ragChatUseCase, never()).chatStream(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should propagate stream error from service")
    void shouldPropagateStreamErrorFromService() {
      when(ragChatUseCase.chatStream(any(), any(), any(), any()))
          .thenReturn(Flux.error(new RuntimeException("Service error")));

      assertThat(
              mvc.post()
                  .uri("/api/rag/chat/stream")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"Question\"}")
                  .exchange(Duration.ofSeconds(5)))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      verify(ragChatUseCase).chatStream(eq("Question"), isNull(), eq(5), isNull());
    }
  }

  private Document createTestDocument(String title, DocumentStatus status) {
    return new Document(DocumentId.generate(), title, title, 1024L, "c:test");
  }
}
