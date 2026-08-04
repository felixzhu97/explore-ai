package com.ai.rag.domain;

import com.ai.rag.domain.RawDocument;

/**
 * Reads raw content from a source into a RawDocument.
 */
public interface DocumentReader {

    RawDocument read(byte[] content, String fileName);

    default RawDocument read(String content, String fileName) {
        return read(content.getBytes(), fileName);
    }
}
