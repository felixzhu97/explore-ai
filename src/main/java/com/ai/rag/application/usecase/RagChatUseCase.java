package com.ai.rag.application.usecase;

import com.ai.chat.domain.service.LanguageDetectionService;
import com.ai.common.application.llm.ChatClientProfile;
import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.util.LogSanitizer;
import com.ai.rag.application.dto.RagChatResult;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.retrieval.H2DocumentRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    private final RagApplicationService ragApplicationService;
    private final ChatClientProvider chatClientProvider;
    private final LanguageDetectionService languageDetectionService;
    private final H2DocumentRetriever documentRetriever;

    public RagChatUseCase(
            RagApplicationService ragApplicationService,
            ChatClientProvider chatClientProvider,
            LanguageDetectionService languageDetectionService,
            H2DocumentRetriever documentRetriever) {
        this.ragApplicationService = ragApplicationService;
        this.chatClientProvider = chatClientProvider;
        this.languageDetectionService = languageDetectionService;
        this.documentRetriever = documentRetriever;
    }

    public RagChatResult chat(String question, List<String> docIds, Integer topK) {
        return chat(question, docIds, topK, null);
    }

    public RagChatResult chat(String question, List<String> docIds, Integer topK, String sessionId) {
        log.info("RAG chat request: {}", LogSanitizer.truncate(question));

        List<DocumentId> docIdList = null;
        if (docIds != null && !docIds.isEmpty()) {
            docIdList = docIds.stream().map(DocumentId::of).toList();
        }

        int topKValue = topK != null ? topK : DEFAULT_TOP_K;
        // Keep sources for API response; advisor retrieves again for prompt augmentation.
        var retrievalResult = ragApplicationService.retrieveContext(question, docIdList, topKValue);
        List<SourceDocument> sources = retrievalResult.sources();

        String languageCode = languageDetectionService.detect(question);
        String languageHint = "Respond in the same language as the user question (detected: "
                + languageCode + ").";

        ChatClientProfile profile = sessionId != null && !sessionId.isBlank()
                ? ChatClientProfile.MEMORY
                : ChatClientProfile.BARE;
        ChatClient chatClient = chatClientProvider.create(
                TextChatOptions.withoutTools(), profile, sessionId);

        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();

        var promptSpec = chatClient.prompt()
                .advisors(ragAdvisor)
                .system(languageHint)
                .user(question);

        if (sessionId != null && !sessionId.isBlank()) {
            promptSpec = promptSpec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId));
        }

        String aiResponse = promptSpec.call().content();

        log.info("RAG chat completed successfully");
        return new RagChatResult(aiResponse, sources);
    }
}
