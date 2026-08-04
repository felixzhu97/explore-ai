package com.ai.rag.domain;

import java.util.UUID;

/**
 * Exception thrown when a document is not found.
 */
public class DocumentNotFoundException extends RagServiceException {

    public DocumentNotFoundException(UUID id) {
        super("Document not found: " + id);
    }
}
