package com.ai.pipeline.domain.exception;

public class SavedAgentTypeConflictException extends RuntimeException {

    public SavedAgentTypeConflictException(String typeKey) {
        super("Agent type key already exists: " + typeKey);
    }
}
