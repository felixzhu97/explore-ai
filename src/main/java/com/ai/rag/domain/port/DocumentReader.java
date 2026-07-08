package com.ai.rag.domain.port;

import com.ai.rag.domain.model.RawDocument;

/**
 * DocumentReader port - reads raw content from a source into a RawDocument.
 * Implementations belong to the outer infrastructure layer.
 */
public interface DocumentReader {

    /**
     * Read content from bytes and fileName.
     */
    RawDocument read(byte[] content, String fileName);

    /**
     * Read content from String.
     */
    default RawDocument read(String content, String fileName) {
        return read(content.getBytes(), fileName);
    }
}
