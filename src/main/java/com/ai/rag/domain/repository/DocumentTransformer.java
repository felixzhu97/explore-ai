package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.ExtractedDocument;
import java.util.List;

/** Transforms raw documents into processed chunks. */
public interface DocumentTransformer {
  /** Splits a raw document into smaller documents. */
  List<ExtractedDocument> splitDocument(ExtractedDocument document);
}
