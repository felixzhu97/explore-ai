package com.ai.rag.domain.exception;

/** Base failure of the RAG pipeline, such as embedding errors or missing documents. */
public class RagServiceException extends RuntimeException {
  public RagServiceException(String message) {
    super(message);
  }

  public RagServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
