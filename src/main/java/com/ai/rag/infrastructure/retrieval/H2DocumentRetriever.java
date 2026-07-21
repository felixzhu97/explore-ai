package com.ai.rag.infrastructure.retrieval;

import com.ai.rag.application.usecase.DocumentSearchService;
import com.ai.rag.domain.model.SourceDocument;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts existing H2 vector search to Spring AI DocumentRetriever for RetrievalAugmentationAdvisor.
 */
@Component
public class H2DocumentRetriever implements DocumentRetriever {

    private final DocumentSearchService documentSearchService;

    public H2DocumentRetriever(DocumentSearchService documentSearchService) {
        this.documentSearchService = documentSearchService;
    }

    @Override
    public List<Document> retrieve(Query query) {
        DocumentSearchService.RetrievalResult result =
                documentSearchService.retrieve(query.text(), null, 0);
        return result.sources().stream()
                .map(this::toDocument)
                .toList();
    }

    private Document toDocument(SourceDocument source) {
        Map<String, Object> metadata = new HashMap<>();
        if (source.metadata() != null) {
            metadata.putAll(source.metadata());
        }
        metadata.put("score", source.score());
        return new Document(source.text(), metadata);
    }
}
