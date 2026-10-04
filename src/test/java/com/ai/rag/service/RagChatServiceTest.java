package com.ai.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.service.dto.RagChatResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatService")
class RagChatServiceTest {

  private static final String OWNER = "c:owner";

  @Mock private ChatClientProvider chatClientProvider;

  @Mock private ChatClient chatClient;

  @Mock private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.CallResponseSpec callResponseSpec;

  @Mock private LanguageDetectionService languageDetectionService;

  @Mock private VectorStore vectorStore;

  @Mock private RagRetrievalSettings retrievalSettings;

  @Mock private AiInvocationRecorder invocationRecorder;

  private RagChatService ragChatService;

  @BeforeEach
  void setUp() {
    when(retrievalSettings.getScoreThreshold()).thenReturn(0.5);
    ragChatService =
        new RagChatService(
            chatClientProvider,
            languageDetectionService,
            vectorStore,
            retrievalSettings,
            invocationRecorder,
            new ObjectMapper());
    when(chatClientProvider.create(any(TextChatOptions.class), any(ChatClientProfile.class), any()))
        .thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.advisors(any(Advisor.class))).thenReturn(requestSpec);
    when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
    when(requestSpec.system(anyString())).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(languageDetectionService.detect(anyString())).thenReturn("en");
  }

  @Nested
  @DisplayName("chat()")
  class Chat {

    @Test
    @DisplayName("should return chat result with response and sources when question provided")
    void shouldReturnChatResultWithResponseAndSourcesWhenQuestionProvided() {
      String question = "What is AI?";
      String aiResponse = "AI is Artificial Intelligence";
      Document sourceDoc = new Document("AI definition", Map.of("score", 0.95));
      stubChatClientResponse(aiResponse, List.of(sourceDoc));

      RagChatResult result = ragChatService.chat(question, null, 5, null, OWNER);

      assertThat(result).isNotNull();
      assertThat(result.response()).isEqualTo(aiResponse);
      assertThat(result.sources()).hasSize(1);
      assertThat(result.sources().getFirst().content()).isEqualTo("AI definition");
      assertThat(result.sources().getFirst().score()).isEqualTo(0.95);
      verify(requestSpec).user(question);
    }

    @Test
    @DisplayName("should filter by owner and doc ids when doc ids are provided")
    void shouldFilterByOwnerAndDocIdsWhenDocIdsAreProvided() {
      String docId = UUID.randomUUID().toString();
      stubChatClientResponse("response", List.of());

      ragChatService.chat("What is AI?", List.of(docId), 5, null, OWNER);

      FilterExpressionBuilder b = new FilterExpressionBuilder();
      assertThat(capturedFilter())
          .isEqualTo(
              b.and(b.eq("ownerKey", OWNER), b.in("document_id", List.<Object>of(docId))).build());
    }

    @Test
    @DisplayName("should filter by owner only when doc ids are empty")
    void shouldFilterByOwnerOnlyWhenDocIdsAreEmpty() {
      stubChatClientResponse("response", List.of());

      ragChatService.chat("q", Collections.emptyList(), 10, null, OWNER);

      assertThat(capturedFilter())
          .isEqualTo(new FilterExpressionBuilder().eq("ownerKey", OWNER).build());
    }

    @Test
    @DisplayName("should use bare client for query compression when session id is present")
    void shouldUseBareClientForQueryCompressionWhenSessionIdIsPresent() {
      ChatClient compressionClient = mock(ChatClient.class);
      ChatClient.Builder compressionBuilder = mock(ChatClient.Builder.class);
      when(chatClientProvider.createBareStateless(any(TextChatOptions.class)))
          .thenReturn(compressionClient);
      when(compressionClient.mutate()).thenReturn(compressionBuilder);
      when(compressionBuilder.build()).thenReturn(compressionClient);
      stubChatClientResponse("response", List.of());

      ragChatService.chat("follow-up question", null, 5, "session-1", OWNER);

      verify(chatClientProvider)
          .create(any(TextChatOptions.class), eq(ChatClientProfile.MEMORY), eq("session-1"));
      verify(compressionClient).mutate();
      verify(chatClient, never()).mutate();
    }
  }

  private Object capturedFilter() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
        ArgumentCaptor.forClass(Consumer.class);
    verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());
    CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
    advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
    return capturing.params.get(VectorStoreDocumentRetriever.FILTER_EXPRESSION);
  }

  private void stubChatClientResponse(String content, List<Document> documents) {
    ChatResponse chatResponse =
        ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(content))))
            .build();
    ChatClientResponse clientResponse =
        ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(Map.of(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, documents))
            .build();
    when(callResponseSpec.chatClientResponse()).thenReturn(clientResponse);
  }

  private static final class CapturingAdvisorSpec implements ChatClient.AdvisorSpec {
    private final Map<String, Object> params = new java.util.HashMap<>();

    @Override
    public ChatClient.AdvisorSpec param(String key, Object value) {
      params.put(key, value);
      return this;
    }

    @Override
    public ChatClient.AdvisorSpec params(Map<String, Object> p) {
      params.putAll(p);
      return this;
    }

    @Override
    public ChatClient.AdvisorSpec advisors(Advisor... advisors) {
      return this;
    }

    @Override
    public ChatClient.AdvisorSpec advisors(List<Advisor> advisors) {
      return this;
    }
  }
}
