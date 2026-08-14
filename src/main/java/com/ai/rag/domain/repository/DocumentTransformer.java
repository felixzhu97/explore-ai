package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.RawDocument;
import java.util.List;

/** Transforms raw documents into processed chunks. */
public interface DocumentTransformer {
  /** Documentation. */
  List<RawDocument> transform(RawDocument document);
}
