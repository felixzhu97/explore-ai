package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.ExtractedDocument;

/** Reads raw content from a source into a ExtractedDocument. */
public interface DocumentReader {
  /** Reads a file into a raw document. */
  ExtractedDocument read(byte[] content, String fileName);

  /** Reads text into a raw document. */
  default ExtractedDocument read(String content, String fileName) {
    return read(content.getBytes(), fileName);
  }
}
