package com.ai.rag.service.dto;

import com.ai.rag.domain.model.SourceCitation;
import java.util.List;
import java.util.Map;

/**
 * One retrieved chunk in the {@code sources} SSE event.
 *
 * @param metadata vector-store metadata; keys vary by ingestion source, so it stays an open map
 */
public record RagSourceEvent(String content, double score, Map<String, Object> metadata) {

  /** Maps retrieved documents to events, skipping chunks without text. */
  public static List<RagSourceEvent> fromAll(List<SourceCitation> sources) {
    return sources.stream()
        .filter(source -> source.content() != null && !source.content().isBlank())
        .map(
            source ->
                new RagSourceEvent(
                    source.content(),
                    source.score(),
                    source.metadata() == null ? Map.of() : source.metadata()))
        .toList();
  }
}
