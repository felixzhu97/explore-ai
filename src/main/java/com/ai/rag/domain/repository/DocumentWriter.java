package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.DocumentChunk;
import java.util.List;

/**
 * Writes processed chunks to persistent storage.
 */
public interface DocumentWriter {

    void write(List<DocumentChunk> chunks);
}
