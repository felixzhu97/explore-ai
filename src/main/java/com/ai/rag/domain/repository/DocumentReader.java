package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.RawDocument;

/** Reads raw content from a source into a RawDocument. */
public interface DocumentReader {
  /** Documentation. */
  RawDocument read(byte[] content, String fileName);

  /** Documentation. */
  default RawDocument read(String content, String fileName) {
    return read(content.getBytes(), fileName);
  }
}
