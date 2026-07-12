package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.RawDocument;

/**
 * Reads raw content from a source into a RawDocument.
 */
public interface DocumentReader {

    RawDocument read(byte[] content, String fileName);

    default RawDocument read(String content, String fileName) {
        return read(content.getBytes(), fileName);
    }
}
