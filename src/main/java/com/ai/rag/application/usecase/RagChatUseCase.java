package com.ai.rag.application.usecase;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.application.llm.ChatClientProfile;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.util.LogSanitizer;
import com.ai.metrics.application.AiInvocationRecorder;
import com.ai.metrics.domain.model.AiInvocationEvent;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.InvocationOutcome;
import com.ai.rag.application.dto.RagChatResult;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.infrastructure.retrieval.H2DocumentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    private final ChatClientProvider chatClientProvider;
    private final LanguageDetectionService languageDetectionService;
    private final H2DocumentRetriever documentRetriever;
    private final AiInvocationRecorder invocationRecorder;

    public RagChatUseCase(
            ChatClientProvider chatClientProvider,
            LanguageDetectionService languageDetectionService,
            H2DocumentRetriever documentRetriever,
            AiInvocationRecorder invocationRecorder) {
        this.chatClientProvider = chatClientProvider;
        this.languageDetectionService = languageDetectionService;
        this.documentRetriever = documentRetriever;
        this.invocationRecorder = invocationRecorder;
    }

    public RagChatResult chat(String question, List<String> docIds, Integer topK) {
        return chat(question, docIds, topK, null);
    }

    public RagChatResult chat(String question, List<String> docIds, Integer topK, String sessionId) {
        log.info("RAG chat request: {}", LogSanitizer.truncate(question));
        long startedAt = System.nanoTime();
        String documentId = docIds != null && !docIds.isEmpty() ? docIds.getFirst() : null;

        try {
            int topKValue = topK != null ? topK : DEFAULT_TOP_K;
            List<String> filterDocIds = docIds != null && !docIds.isEmpty() ? List.copyOf(docIds) : null;

            String languageCode = languageDetectionService.detect(question);
            String languageHint = "Respond in the same language as the user question (detected: "
                    + languageCode + ").";

            ChatClientProfile profile = sessionId != null && !sessionId.isBlank()
                    ? ChatClientProfile.MEMORY
                    : ChatClientProfile.BARE;
            TextChatOptions options = TextChatOptions.withoutTools();
            ChatClient chatClient = chatClientProvider.create(options, profile, sessionId);

            RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(documentRetriever)
                    .build();

            var promptSpec = chatClient.prompt()
                    .advisors(ragAdvisor)
                    .advisors(a -> {
                        a.param(H2DocumentRetriever.TOP_K_CONTEXT_KEY, topKValue);
                        if (filterDocIds != null) {
                            a.param(H2DocumentRetriever.DOC_IDS_CONTEXT_KEY, filterDocIds);
                        }
                        if (sessionId != null && !sessionId.isBlank()) {
                            a.param(ChatMemory.CONVERSATION_ID, sessionId);
                        }
                    })
                    .system(languageHint)
                    .user(question);

            ChatClientResponse clientResponse = promptSpec.call().chatClientResponse();
            String aiResponse = extractContent(clientResponse);
            List<SourceDocument> sources = extractSources(clientResponse);

            invocationRecorder.record(AiInvocationEvent.builder()
                    .domain(AiDomain.RAG)
                    .operation("rag.chat")
                    .outcome(InvocationOutcome.SUCCESS)
                    .latencyMs((System.nanoTime() - startedAt) / 1_000_000L)
                    .provider(options.provider())
                    .model(options.model())
                    .sessionId(sessionId)
                    .documentId(documentId)
                    .build());

            log.info("RAG chat completed successfully");
            return new RagChatResult(aiResponse, sources);
        } catch (RuntimeException ex) {
            invocationRecorder.recordError(
                    AiDomain.RAG,
                    "rag.chat",
                    (System.nanoTime() - startedAt) / 1_000_000L,
                    "openai",
                    null,
                    sessionId,
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw ex;
        }
    }

    private static String extractContent(ChatClientResponse clientResponse) {
        ChatResponse chatResponse = clientResponse.chatResponse();
        if (chatResponse == null) {
            return "";
        }
        Generation generation = chatResponse.getResult();
        if (generation == null) {
            return "";
        }
        AssistantMessage output = generation.getOutput();
        return output != null && output.getText() != null ? output.getText() : "";
    }

    @SuppressWarnings("unchecked")
    private static List<SourceDocument> extractSources(ChatClientResponse clientResponse) {
        Object raw = clientResponse.context().get(RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT);
        if (!(raw instanceof List<?> documents) || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .map(RagChatUseCase::toSourceDocument)
                .toList();
    }

    private static SourceDocument toSourceDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata() != null
                ? new HashMap<>(document.getMetadata())
                : new HashMap<>();
        double score = 0.0;
        Object scoreMeta = metadata.get("score");
        if (scoreMeta instanceof Number number) {
            score = number.doubleValue();
        } else if (document.getScore() != null) {
            score = document.getScore();
        }
        return new SourceDocument(
                document.getText() != null ? document.getText() : "",
                score,
                Collections.unmodifiableMap(metadata));
    }
}
