package com.ai.rag.domain.port;

import com.ai.rag.domain.model.DocumentChunk;

import java.util.List;

/**
 * DocumentWriter port - writes processed chunks to persistent storage.
 * Implementations belong to the outer infrastructure layer.
 */
public interface DocumentWriter {

    /**
     * Write document chunks to storage.
     */
    void write(List<DocumentChunk> chunks);
}
