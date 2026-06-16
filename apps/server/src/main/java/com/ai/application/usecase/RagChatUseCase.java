package com.ai.application.usecase;

import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.ChatMessage;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.RagContextFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RagChatUseCase {
    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_HISTORY_MESSAGES = 10;

    private final EmbeddingPort embeddingPort;
    private final VectorSearchPort vectorSearchPort;

    public RagChatUseCase(EmbeddingPort embeddingPort, VectorSearchPort vectorSearchPort) {
        this.embeddingPort = embeddingPort;
        this.vectorSearchPort = vectorSearchPort;
    }

    public record RetrievalResult(
        String context,
        List<SourceDocument> sources,
        String enrichedQuery
    ) {}

    public RetrievalResult execute(String query, List<UUID> docIds, int topK, List<ChatMessage> history) {
        String enrichedQuery = enrichWithHistory(query, history);
        log.info("RAG retrieval for query: {}", enrichedQuery);

        float[] queryEmbedding = embeddingPort.embed(enrichedQuery);

        List<DocumentChunk> chunks;
        if (docIds != null && !docIds.isEmpty()) {
            chunks = vectorSearchPort.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K, docIds);
        } else {
            chunks = vectorSearchPort.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K);
        }

        List<SourceDocument> sources = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            int sourceIndex = i + 1;

            SourceDocument source = RagContextFormatter.formatSource(chunk, sourceIndex, queryEmbedding);
            sources.add(source);
        }

        String context = RagContextFormatter.buildContextWithSources(chunks, queryEmbedding);

        log.info("Retrieved {} chunks", chunks.size());
        return new RetrievalResult(context, sources, enrichedQuery);
    }

    public RetrievalResult execute(String query, List<UUID> docIds, int topK) {
        return execute(query, docIds, topK, null);
    }

    private String enrichWithHistory(String query, List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return query;
        }

        int historyLimit = Math.min(history.size(), MAX_HISTORY_MESSAGES);
        List<ChatMessage> recentHistory = history.subList(history.size() - historyLimit, history.size());

        StringBuilder historyContext = new StringBuilder();
        for (ChatMessage msg : recentHistory) {
            historyContext.append(msg.getRole()).append(": ").append(msg.getText()).append("\n");
        }

        return "Previous conversation:\n" + historyContext +
               "\nCurrent question: " + query;
    }
}
