package com.ai.rag.application.usecase;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.application.llm.ChatClientProfile;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.rag.application.dto.RagChatResult;
import com.ai.rag.infrastructure.retrieval.H2DocumentRetriever;
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
    private H2DocumentRetriever documentRetriever;

    @Mock
    private AiInvocationRecorder invocationRecorder;

    private RagChatUseCase ragChatUseCase;

    @BeforeEach
    void setUp() {
        ragChatUseCase = new RagChatUseCase(
                chatClientProvider,
                languageDetectionService,
                documentRetriever,
                invocationRecorder
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
        @DisplayName("should_passDocIdsViaAdvisorParams_when_provided")
        void should_passDocIdsViaAdvisorParams_when_provided() {
            String question = "What is AI?";
            String docId1 = UUID.randomUUID().toString();
            List<String> docIds = List.of(docId1);
            stubChatClientResponse("response", List.of());

            ragChatUseCase.chat(question, docIds, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
                    ArgumentCaptor.forClass(Consumer.class);
            verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());

            CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
            advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
            assertThat(capturing.params)
                    .containsEntry(H2DocumentRetriever.TOP_K_CONTEXT_KEY, 5)
                    .containsEntry(H2DocumentRetriever.DOC_IDS_CONTEXT_KEY, docIds);
        }

        @Test
        @DisplayName("should_useCustomTopK_when_provided")
        void should_useCustomTopK_when_provided() {
            String question = "What is AI?";
            int customTopK = 10;
            stubChatClientResponse("response", List.of());

            ragChatUseCase.chat(question, null, customTopK);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
                    ArgumentCaptor.forClass(Consumer.class);
            verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());

            CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
            advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
            assertThat(capturing.params).containsEntry(H2DocumentRetriever.TOP_K_CONTEXT_KEY, customTopK);
            assertThat(capturing.params).doesNotContainKey(H2DocumentRetriever.DOC_IDS_CONTEXT_KEY);
        }

        @Test
        @DisplayName("should_useDefaultTopK_when_topKIsNull")
        void should_useDefaultTopK_when_topKIsNull() {
            String question = "What is AI?";
            stubChatClientResponse("response", List.of());

            ragChatUseCase.chat(question, null, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
                    ArgumentCaptor.forClass(Consumer.class);
            verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());

            CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
            advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
            assertThat(capturing.params).containsEntry(H2DocumentRetriever.TOP_K_CONTEXT_KEY, 5);
        }

        @Test
        @DisplayName("should_handleEmptyDocIdsList_when_empty")
        void should_handleEmptyDocIdsList_when_empty() {
            String question = "What is AI?";
            List<String> emptyDocIds = Collections.emptyList();
            stubChatClientResponse("response", List.of());

            ragChatUseCase.chat(question, emptyDocIds, null);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<ChatClient.AdvisorSpec>> advisorCaptor =
                    ArgumentCaptor.forClass(Consumer.class);
            verify(requestSpec, atLeastOnce()).advisors(advisorCaptor.capture());

            CapturingAdvisorSpec capturing = new CapturingAdvisorSpec();
            advisorCaptor.getAllValues().forEach(consumer -> consumer.accept(capturing));
            assertThat(capturing.params).doesNotContainKey(H2DocumentRetriever.DOC_IDS_CONTEXT_KEY);
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
