package com.ai.rag.domain.port;

import com.ai.rag.domain.model.RawDocument;

import java.util.List;

/**
 * DocumentTransformer port - transforms raw documents into processed chunks.
 * Implementations belong to the outer infrastructure layer.
 */
public interface DocumentTransformer {

    /**
     * Transform a raw document into a list of processed documents.
     */
    List<RawDocument> transform(RawDocument document);
}
