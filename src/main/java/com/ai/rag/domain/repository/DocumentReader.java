package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.RawDocument;

/** Reads raw content from a source into a RawDocument. */
public interface DocumentReader {
  /** Reads a file into a raw document. */
  RawDocument read(byte[] content, String fileName);

  /** Reads text into a raw document. */
  default RawDocument read(String content, String fileName) {
    return read(content.getBytes(), fileName);
  }
}
