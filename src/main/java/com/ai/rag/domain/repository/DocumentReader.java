package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.ExtractedDocument;

/** Reads raw content from a source into a ExtractedDocument. */
public interface DocumentReader {
  /** Reads a file into a raw document. */
  ExtractedDocument readDocument(byte[] content, String fileName);

  /** Reads text into a raw document. */
  default ExtractedDocument readDocument(String content, String fileName) {
    return readDocument(content.getBytes(), fileName);
  }
}
