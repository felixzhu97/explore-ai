package com.ai.rag.domain;

import com.ai.rag.domain.DocumentChunk;
import java.util.List;

/**
 * Writes processed chunks to persistent storage.
 */
public interface DocumentWriter {

    void write(List<DocumentChunk> chunks);
}
