package com.ai.rag.application.usecase;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.application.llm.ChatClientProfile;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.rag.application.dto.RagChatResult;
import com.ai.rag.domain.repository.RagRetrievalSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagChatUseCase")
class RagChatUseCaseTest {

    @Mock
    private ChatClientProvider chatClientProvider;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private LanguageDetectionService languageDetectionService;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private RagRetrievalSettings retrievalSettings;

    @Mock
    private AiInvocationRecorder invocationRecorder;

    private RagChatUseCase ragChatUseCase;

    @BeforeEach
    void setUp() {
        when(retrievalSettings.getScoreThreshold()).thenReturn(0.5);
        ragChatUseCase = new RagChatUseCase(
                chatClientProvider,
                languageDetectionService,
                vectorStore,
                retrievalSettings,
                invocationRecorder,
                new ObjectMapper()
        );
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
        @DisplayName("should_returnChatResultWithResponseAndSources_when_questionProvided")
        void should_returnChatResultWithResponseAndSources_when_questionProvided() {
            String question = "What is AI?";
            String aiResponse = "AI is Artificial Intelligence";
            Document sourceDoc = new Document("AI definition", Map.of("score", 0.95));
            stubChatClientResponse(aiResponse, List.of(sourceDoc));

            RagChatResult result = ragChatUseCase.chat(question, null, null);

            assertThat(result).isNotNull();
            assertThat(result.response()).isEqualTo(aiResponse);
            assertThat(result.sources()).hasSize(1);
            assertThat(result.sources().getFirst().text()).isEqualTo("AI definition");
            assertThat(result.sources().getFirst().score()).isEqualTo(0.95);
            verify(requestSpec).user(question);
        }

        @Test
        @DisplayName("should_passDocIdsViaFilterExpression_when_provided")
        void should_passDocIdsViaFilterExpression_when_provided() {
            String question = "What is AI?";
            String docId1 = UUID.randomUUID().toString();
            stubChatClientResponse("response", List.of());

            ragChatUseCase.chat(question, List.of(docId1), null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
                    ArgumentCaptor.forClass(Consumer.class);
            verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());

            CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
            advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
            assertThat(capturing.params).containsKey(VectorStoreDocumentRetriever.FILTER_EXPRESSION);
            assertThat(capturing.params.get(VectorStoreDocumentRetriever.FILTER_EXPRESSION))
                    .isInstanceOf(Filter.Expression.class);
        }

        @Test
        @DisplayName("should_omitFilter_when_docIdsEmpty")
        void should_omitFilter_when_docIdsEmpty() {
            stubChatClientResponse("response", List.of());

            ragChatUseCase.chat("q", Collections.emptyList(), 10);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
                    ArgumentCaptor.forClass(Consumer.class);
            verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());

            CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
            advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
            assertThat(capturing.params).doesNotContainKey(VectorStoreDocumentRetriever.FILTER_EXPRESSION);
        }
    }

    private void stubChatClientResponse(String content, List<Document> documents) {
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(content))))
                .build();
        ChatClientResponse clientResponse = ChatClientResponse.builder()
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
