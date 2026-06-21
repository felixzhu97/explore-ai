package com.ai.modules.rag.application.usecase;

import com.ai.modules.rag.domain.model.SourceDocument;
import com.ai.modules.ai.application.usecase.AiChatUseCase;
import com.ai.domain.service.LanguageDetectionService;
import com.ai.domain.service.PromptTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Application layer use case for RAG chat operations.
 * Orchestrates retrieval, prompt building, and AI chat into a single workflow.
 */
@Service
public class RagChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    private final RagApplicationService ragApplicationService;
    private final AiChatUseCase aiChatUseCase;
    private final LanguageDetectionService languageDetectionService;
    private final PromptTemplates promptTemplates;

    public RagChatUseCase(
            RagApplicationService ragApplicationService,
            AiChatUseCase aiChatUseCase,
            LanguageDetectionService languageDetectionService,
            PromptTemplates promptTemplates) {
        this.ragApplicationService = ragApplicationService;
        this.aiChatUseCase = aiChatUseCase;
        this.languageDetectionService = languageDetectionService;
        this.promptTemplates = promptTemplates;
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

        List<UUID> docUuids = null;
        if (docIds != null && !docIds.isEmpty()) {
            docUuids = docIds.stream()
                    .map(UUID::fromString)
                    .toList();
        }

        int topKValue = topK != null ? topK : DEFAULT_TOP_K;
        var retrievalResult = ragApplicationService.retrieveContext(question, docUuids, topKValue);

        String context = retrievalResult.context();
        List<SourceDocument> sources = retrievalResult.sources();

        String prompt = buildPrompt(question, context);
        String aiResponse = aiChatUseCase.chat(prompt);

        log.info("RAG chat completed successfully");
        return new ChatResult(aiResponse, sources);
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        languageDetectionService.buildPrompt(question, context, languageCode);
        return promptTemplates.buildQuestionAnswerPrompt(context, question);
    }
}
