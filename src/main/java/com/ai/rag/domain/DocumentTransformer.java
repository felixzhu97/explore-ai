package com.ai.rag.domain;

import com.ai.rag.domain.RawDocument;
import java.util.List;

/**
 * Transforms raw documents into processed chunks.
 */
public interface DocumentTransformer {

    List<RawDocument> transform(RawDocument document);
}
