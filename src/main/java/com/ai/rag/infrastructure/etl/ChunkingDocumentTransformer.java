package com.ai.rag.infrastructure.etl;

import com.ai.rag.domain.model.RawDocument;
import com.ai.rag.domain.repository.DocumentTransformer;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/** Infrastructure adapter: splits documents with Spring AI {@link TokenTextSplitter}. */
@Component
public class ChunkingDocumentTransformer implements DocumentTransformer {

  private final TokenTextSplitter textSplitter;

  /** Documentation. */
  public ChunkingDocumentTransformer(TokenTextSplitter textSplitter) {
    this.textSplitter = textSplitter;
  }

  @Override
  public List<RawDocument> transform(RawDocument document) {
    if (document.content() == null || document.content().isBlank()) {
      return List.of();
    }
    Document springDocument = new Document(document.content(), document.metadata());
    return textSplitter.apply(List.of(springDocument)).stream()
        .map(chunk -> new RawDocument(chunk.getText(), document.metadata(), document.source()))
        .toList();
  }
}
