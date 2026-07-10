package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.chat.infrastructure.llm.ChatClientFactory;
import com.ai.chat.application.usecase.TextChatOptions;
import com.ai.chat.domain.service.LanguageDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    private final RagApplicationService ragApplicationService;
    private final ChatClientFactory chatClientFactory;
    private final LanguageDetectionService languageDetectionService;

    public RagChatUseCase(
            RagApplicationService ragApplicationService,
            ChatClientFactory chatClientFactory,
            LanguageDetectionService languageDetectionService) {
        this.ragApplicationService = ragApplicationService;
        this.chatClientFactory = chatClientFactory;
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
        String prompt = buildPrompt(question, retrievalResult.context());

        ChatClient chatClient = chatClientFactory.createStateless(TextChatOptions.defaults());
        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.info("RAG chat completed successfully");
        return new ChatResult(aiResponse, sources);
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        return languageDetectionService.buildPrompt(question, context, languageCode);
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
