package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.RawDocument;
import java.util.List;

/**
 * Transforms raw documents into processed chunks.
 */
public interface DocumentTransformer {

    List<RawDocument> transform(RawDocument document);
}
