package com.ai.pipeline.domain.exception;

public class SavedAgentNotFoundException extends RuntimeException {

    public SavedAgentNotFoundException(String id) {
        super("Saved agent not found: " + id);
    }
}
