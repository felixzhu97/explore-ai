package com.ai.rag.infrastructure.retrieval;

import com.ai.rag.application.usecase.DocumentSearchService;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.stereotype.Component;

/**
 * Adapts existing H2 vector search to Spring AI DocumentRetriever for RetrievalAugmentationAdvisor.
 * Reads optional topK / docIds from {@link Query#context()} (advisor request params).
 */
@Component
public class H2DocumentRetriever implements DocumentRetriever {

  public static final String TOP_K_CONTEXT_KEY = "topK";
  public static final String DOC_IDS_CONTEXT_KEY = "docIds";

  private final DocumentSearchService documentSearchService;

  /** Documentation. */
  public H2DocumentRetriever(DocumentSearchService documentSearchService) {
    this.documentSearchService = documentSearchService;
  }

  @Override
  public List<Document> retrieve(Query query) {
    Map<String, Object> context = query.context() != null ? query.context() : Map.of();
    int topK = resolveTopK(context.get(TOP_K_CONTEXT_KEY));
    List<DocumentId> docIds = resolveDocIds(context.get(DOC_IDS_CONTEXT_KEY));

    DocumentSearchService.RetrievalResult result =
        documentSearchService.retrieve(query.text(), docIds, topK);
    return result.sources().stream().map(this::toDocument).toList();
  }

  private static int resolveTopK(Object raw) {
    if (raw instanceof Number number) {
      return number.intValue();
    }
    if (raw instanceof String text && !text.isBlank()) {
      try {
        return Integer.parseInt(text.trim());
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }

  @SuppressWarnings("unchecked")
  private static List<DocumentId> resolveDocIds(Object raw) {
    if (!(raw instanceof List<?> list) || list.isEmpty()) {
      return null;
    }
    List<DocumentId> docIds = new ArrayList<>(list.size());
    for (Object item : list) {
      if (item == null) {
        continue;
      }
      docIds.add(DocumentId.of(item.toString()));
    }
    return docIds.isEmpty() ? null : docIds;
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
