package com.ai.rag.domain.exception;

public class RagServiceException extends RuntimeException {
    public RagServiceException(String message) {
        super(message);
    }
    public RagServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
