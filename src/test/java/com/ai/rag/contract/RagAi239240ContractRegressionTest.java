package com.ai.rag.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.ai.rag.service.usecase.RagApplicationService;
import com.ai.rag.service.usecase.RagChatUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.lang.reflect.Field;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AI-239/240 RAG contract regression")
class RagAi239240ContractRegressionTest {

  private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("com.ai.rag");

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private ChatClient chatClient;
  @Mock private ChatClient.ChatClientRequestSpec requestSpec;
  @Mock private ChatClient.CallResponseSpec callResponseSpec;
  @Mock private ChatClient.StreamResponseSpec streamResponseSpec;
  @Mock private LanguageDetectionService languageDetectionService;
  @Mock private VectorStore vectorStore;
  @Mock private RagRetrievalSettings retrievalSettings;
  @Mock private AiInvocationRecorder invocationRecorder;
  private RagChatUseCase ragChatUseCase;

  @BeforeEach
  void setUp() {
    when(retrievalSettings.getScoreThreshold()).thenReturn(0.5);
    ragChatUseCase =
        new RagChatUseCase(
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
    when(languageDetectionService.detect(anyString())).thenReturn("en");
  }

  @Nested
  @DisplayName("single-pass retrieval + sources (AI-239)")
  class SinglePassRetrieval {
    @Test
    @DisplayName("should not depend on rag application service when rag chat use case")
    void shouldNotDependOnRagApplicationServiceWhenRagChatUseCase() {
      ArchRuleDefinition.noClasses()
          .that()
          .haveSimpleName("RagChatUseCase")
          .should()
          .dependOnClassesThat()
          .haveSimpleName(RagApplicationService.class.getSimpleName())
          .check(CLASSES);
    }

    @Test
    @DisplayName("should use retrieval augmentation advisor when chat")
    void shouldUseRetrievalAugmentationAdvisorWhenChat() {
      when(requestSpec.call()).thenReturn(callResponseSpec);
      when(callResponseSpec.chatClientResponse())
          .thenReturn(clientResponse("answer", List.of(new Document("ctx", Map.of("score", 0.8)))));
      ragChatUseCase.chat("What is AI?", null, null);
      assertThat(captureRetrievalAdvisor()).isPresent();
      verify(requestSpec).call();
    }

    @Test
    @DisplayName("should return sources from document context when chat")
    void shouldReturnSourcesFromDocumentContextWhenChat() {
      when(requestSpec.call()).thenReturn(callResponseSpec);
      when(callResponseSpec.chatClientResponse())
          .thenReturn(
              clientResponse(
                  "answer", List.of(new Document("retrieved chunk", Map.of("score", 0.91)))));
      var result = ragChatUseCase.chat("What is AI?", null, null);
      assertThat(result.sources()).hasSize(1);
      assertThat(result.sources().getFirst().text()).isEqualTo("retrieved chunk");
      assertThat(result.sources().getFirst().score()).isEqualTo(0.91);
    }
  }

  @Nested
  @DisplayName("topK and docIds filter (AI-240)")
  class TopKAndDocIdsFilter {
    @Test
    @DisplayName("should apply custom top k on retriever when top k provided")
    void shouldApplyCustomTopKOnRetrieverWhenTopKProvided() {
      when(requestSpec.call()).thenReturn(callResponseSpec);
      when(callResponseSpec.chatClientResponse()).thenReturn(clientResponse("ok", List.of()));
      ragChatUseCase.chat("q", null, 10);
      assertThat(extractTopK(captureRetrievalAdvisor().orElseThrow())).isEqualTo(10);
    }

    @Test
    @DisplayName("should apply default top k on retriever when top k null")
    void shouldApplyDefaultTopKOnRetrieverWhenTopKNull() {
      when(requestSpec.call()).thenReturn(callResponseSpec);
      when(callResponseSpec.chatClientResponse()).thenReturn(clientResponse("ok", List.of()));
      ragChatUseCase.chat("q", null, null);
      assertThat(extractTopK(captureRetrievalAdvisor().orElseThrow())).isEqualTo(5);
    }

    @Test
    @DisplayName("should pass doc ids via filter expression when doc ids provided")
    void shouldPassDocIdsViaFilterExpressionWhenDocIdsProvided() {
      when(requestSpec.call()).thenReturn(callResponseSpec);
      when(callResponseSpec.chatClientResponse()).thenReturn(clientResponse("ok", List.of()));
      ragChatUseCase.chat("q", List.of(UUID.randomUUID().toString()), null);
      assertThat(captureFilterExpression()).isInstanceOf(Filter.Expression.class);
    }
  }

  @Nested
  @DisplayName("true stream, not fake chunking (AI-240)")
  class TrueStream {
    @Test
    @DisplayName("should stream via chat client when chat stream")
    void shouldStreamViaChatClientWhenChatStream() {
      when(requestSpec.stream()).thenReturn(streamResponseSpec);
      Document source = new Document("source", Map.of("score", 0.7));
      when(streamResponseSpec.chatClientResponse())
          .thenReturn(
              Flux.just(
                  clientResponse("Hello ", List.of()),
                  clientResponse("world", List.of()),
                  clientResponse("", List.of(source))));
      List<ServerSentEvent<String>> events =
          ragChatUseCase.chatStream("q", null, 5, null).collectList().block();
      verify(requestSpec).stream();
      verify(requestSpec, never()).call();
      assertThat(events).hasSize(3);
      assertThat(events.get(0).data()).isEqualTo("Hello ");
      assertThat(events.get(1).data()).isEqualTo("world");
      assertThat(events.stream().filter(e -> "sources".equals(e.event()))).hasSize(1);
    }
  }

  private java.util.Optional<RetrievalAugmentationAdvisor> captureRetrievalAdvisor() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Advisor> advisorCaptor = ArgumentCaptor.forClass(Advisor.class);
    verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());
    return advisorCaptor.getAllValues().stream()
        .filter(RetrievalAugmentationAdvisor.class::isInstance)
        .map(RetrievalAugmentationAdvisor.class::cast)
        .findFirst();
  }

  private Object captureFilterExpression() {
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
        ArgumentCaptor.forClass(Consumer.class);
    verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());
    CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
    advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
    return capturing.params.get(VectorStoreDocumentRetriever.FILTER_EXPRESSION);
  }

  private static int extractTopK(RetrievalAugmentationAdvisor advisor) {
    try {
      Field retrieverField =
          RetrievalAugmentationAdvisor.class.getDeclaredField("documentRetriever");
      retrieverField.setAccessible(true);
      DocumentRetriever retriever = (DocumentRetriever) retrieverField.get(advisor);
      Field topKField = VectorStoreDocumentRetriever.class.getDeclaredField("topK");
      topKField.setAccessible(true);
      return (Integer) topKField.get(retriever);
    } catch (ReflectiveOperationException ex) {
      throw new AssertionError("Failed to read retriever topK from advisor", ex);
    }
  }

  private static ChatClientResponse clientResponse(String content, List<Document> documents) {
    ChatResponse chatResponse =
        ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage(content))))
            .build();
    return ChatClientResponse.builder()
        .chatResponse(chatResponse)
        .context(Map.of(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT, documents))
        .build();
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
