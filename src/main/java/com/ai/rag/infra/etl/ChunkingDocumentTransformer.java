package com.ai.rag.infra.etl;

import com.ai.rag.domain.model.ExtractedDocument;
import com.ai.rag.domain.repository.DocumentTransformer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

/** Infrastructure adapter: splits documents with Spring AI {@link TokenTextSplitter}. */
@Component
@RequiredArgsConstructor
public class ChunkingDocumentTransformer implements DocumentTransformer {

  private final TokenTextSplitter textSplitter;

  @Override
  public List<ExtractedDocument> transform(ExtractedDocument document) {
    if (document.content() == null || document.content().isBlank()) {
      return List.of();
    }
    Document springDocument = new Document(document.content(), document.metadata());
    return textSplitter.apply(List.of(springDocument)).stream()
        .map(
            chunk -> new ExtractedDocument(chunk.getText(), document.metadata(), document.source()))
        .toList();
  }
}
