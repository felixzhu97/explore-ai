package com.ai.rag.application.usecase;

import com.ai.common.application.llm.ChatClientProvider;
import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.observability.AiMetricsRecorder;
import com.ai.common.util.LogSanitizer;
import com.ai.rag.application.dto.RagChatResult;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
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
    private final ChatClientProvider chatClientProvider;
    private final LanguageDetectionService languageDetectionService;
    private final AiMetricsRecorder metricsRecorder;

    public RagChatUseCase(
            RagApplicationService ragApplicationService,
            ChatClientProvider chatClientProvider,
            LanguageDetectionService languageDetectionService,
            AiMetricsRecorder metricsRecorder) {
        this.ragApplicationService = ragApplicationService;
        this.chatClientProvider = chatClientProvider;
        this.languageDetectionService = languageDetectionService;
        this.metricsRecorder = metricsRecorder;
    }

    public RagChatResult chat(String question, List<String> docIds, Integer topK) {
        return metricsRecorder.recordRag(() -> {
            log.info("RAG chat request: {}", LogSanitizer.truncate(question));

            List<DocumentId> docIdList = null;
            if (docIds != null && !docIds.isEmpty()) {
                docIdList = docIds.stream().map(DocumentId::of).toList();
            }

            int topKValue = topK != null ? topK : DEFAULT_TOP_K;
            var retrievalResult = ragApplicationService.retrieveContext(question, docIdList, topKValue);
            metricsRecorder.recordRagRetrieval();
            List<SourceDocument> sources = retrievalResult.sources();
            String prompt = buildPrompt(question, retrievalResult.context());

            ChatClient chatClient = chatClientProvider.createStateless(TextChatOptions.defaults());
            String aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("RAG chat completed successfully");
            return new RagChatResult(aiResponse, sources);
        });
    }

    private String buildPrompt(String question, String context) {
        String languageCode = languageDetectionService.detect(question);
        return languageDetectionService.buildPrompt(question, context, languageCode);
    }
}
