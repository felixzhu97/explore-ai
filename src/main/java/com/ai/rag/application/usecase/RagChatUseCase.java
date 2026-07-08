package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.chat.infrastructure.llm.ChatClientFactory;
import com.ai.chat.application.usecase.TextChatOptions;
import com.ai.chat.domain.service.LanguageDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    private final RagApplicationService ragApplicationService;
    private final ChatClientFactory chatClientFactory;
    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;
    private final LanguageDetectionService languageDetectionService;

    public RagChatUseCase(
            RagApplicationService ragApplicationService,
            ChatClientFactory chatClientFactory,
            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
            LanguageDetectionService languageDetectionService) {
        this.ragApplicationService = ragApplicationService;
        this.chatClientFactory = chatClientFactory;
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.languageDetectionService = languageDetectionService;
    }

    public record ChatResult(
            String response,
            List<SourceDocument> sources
    ) {}

    public ChatResult chat(String question, List<String> docIds, Integer topK) {
        log.info("RAG chat request: {}", truncate(question));

        List<DocumentId> docIdList = null;
        if (docIds != null && !docIds.isEmpty()) {
            docIdList = docIds.stream().map(DocumentId::of).toList();
        }

        int topKValue = topK != null ? topK : DEFAULT_TOP_K;
        var retrievalResult = ragApplicationService.retrieveContext(question, docIdList, topKValue);
        List<SourceDocument> sources = retrievalResult.sources();

        ChatClient chatClient = chatClientFactory.create(TextChatOptions.defaults());
        String aiResponse = chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(question)
                .call()
                .content();

        log.info("RAG chat completed successfully");
        return new ChatResult(aiResponse, sources);
    }

    private String truncate(String question) {
        if (question == null) {
            return "null";
        }
        if (question.length() <= 50) {
            return question;
        }
        return question.substring(0, 50) + "...";
    }
}
