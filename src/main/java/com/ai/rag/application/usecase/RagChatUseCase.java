package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.chat.domain.service.LanguageDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application layer use case for RAG chat operations.
 * Orchestrates retrieval, prompt building, and AI chat into a single workflow.
 */
@Service
public class RagChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    private final RagApplicationService ragApplicationService;
    private final ChatUseCase aiChatUseCase;
    private final LanguageDetectionService languageDetectionService;

    public RagChatUseCase(
            RagApplicationService ragApplicationService,
            ChatUseCase aiChatUseCase,
            LanguageDetectionService languageDetectionService) {
        this.ragApplicationService = ragApplicationService;
        this.aiChatUseCase = aiChatUseCase;
        this.languageDetectionService = languageDetectionService;
    }

    /**
     * Result of RAG chat operation.
     */
    public record ChatResult(
            String response,
            List<SourceDocument> sources
    ) {}

    /**
     * Executes RAG chat: retrieves context, builds prompt, and gets AI response.
     */
    public ChatResult chat(String question, List<String> docIds, Integer topK) {
        log.info("RAG chat request: {}", question != null && question.length() > 50 ? question.substring(0, 50) + "..." : question);

        List<DocumentId> docIdList = null;
        if (docIds != null && !docIds.isEmpty()) {
            docIdList = docIds.stream()
                    .map(DocumentId::of)
                    .toList();
        }

        int topKValue = topK != null ? topK : DEFAULT_TOP_K;
        var retrievalResult = ragApplicationService.retrieveContext(question, docIdList, topKValue);

        String context = retrievalResult.context();
        List<SourceDocument> sources = retrievalResult.sources();

        String prompt = buildPrompt(question, context);
        String aiResponse = aiChatUseCase.chat(prompt);

        log.info("RAG chat completed successfully");
        return new ChatResult(aiResponse, sources);
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        return languageDetectionService.buildPrompt(question, context, languageCode);
    }
}
