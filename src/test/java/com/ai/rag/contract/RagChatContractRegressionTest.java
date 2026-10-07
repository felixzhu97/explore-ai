package com.ai.rag.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ai.common.service.llm.ChatClientProfile;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.rag.infra.config.PropertiesRagRetrievalSettings;
import com.ai.rag.infra.config.RagProperties;
import com.ai.rag.service.RagApplicationService;
import com.ai.rag.service.RagChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
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
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
@DisplayName("RAG chat contract regression")
class RagChatContractRegressionTest {

  private static final String OWNER = "c:owner";

  private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("com.ai.rag");

  @Mock private ChatClientProvider chatClientProvider;
  @Mock private ChatClient chatClient;

  @Mock(answer = Answers.RETURNS_SELF)
  private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock private ChatClient.CallResponseSpec callResponseSpec;
  @Mock private ChatClient.StreamResponseSpec streamResponseSpec;
  @Mock private VectorStore vectorStore;
  @Mock private AiInvocationRecorder invocationRecorder;
  private RagChatService ragChatService;

  @BeforeEach
  void setUp() {
    ragChatService =
        new RagChatService(
            chatClientProvider,
            vectorStore,
            new PropertiesRagRetrievalSettings(new RagProperties()),
            invocationRecorder,
            new ObjectMapper());
  }

  @Test
  @DisplayName("should not depend on rag application service when rag chat service answers")
  void shouldNotDependOnRagApplicationServiceWhenRagChatServiceAnswers() {
    ArchRuleDefinition.noClasses()
        .that()
        .haveSimpleName("RagChatService")
        .should()
        .dependOnClassesThat()
        .haveSimpleName(RagApplicationService.class.getSimpleName())
        .check(CLASSES);
  }

  @Test
  @DisplayName("should retrieve with the requested top k through the augmentation advisor")
  void shouldRetrieveWithTheRequestedTopKThroughTheAugmentationAdvisor() {
    givenChatClient();
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.chatClientResponse())
        .thenReturn(clientResponse("answer", List.of(new Document("ctx", Map.of("score", 0.8)))));

    ragChatService.chatWithDocuments("What is AI?", null, 10, null, OWNER);

    assertThat(extractTopK(captureRetrievalAdvisor().orElseThrow())).isEqualTo(10);
  }

  @Test
  @DisplayName("should stream tokens then sources through the chat client")
  void shouldStreamTokensThenSourcesThroughTheChatClient() {
    givenChatClient();
    when(requestSpec.stream()).thenReturn(streamResponseSpec);
    Document source = new Document("source", Map.of("score", 0.7));
    when(streamResponseSpec.chatClientResponse())
        .thenReturn(
            Flux.just(
                clientResponse("Hello ", List.of()),
                clientResponse("world", List.of()),
                clientResponse("", List.of(source))));

    List<ServerSentEvent<String>> events =
        ragChatService.streamChat("q", null, 5, null, OWNER).collectList().block();

    verify(requestSpec, never()).call();
    assertThat(events).hasSize(3);
    assertThat(events.get(0).data()).isEqualTo("{\"type\":\"message\",\"token\":\"Hello \"}");
    assertThat(events.get(1).data()).isEqualTo("{\"type\":\"message\",\"token\":\"world\"}");
    assertThat(events.stream().filter(e -> "sources".equals(e.event()))).hasSize(1);
  }

  private void givenChatClient() {
    when(chatClientProvider.create(any(TextChatOptions.class), any(ChatClientProfile.class), any()))
        .thenReturn(chatClient);
    when(chatClient.prompt()).thenReturn(requestSpec);
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
}
