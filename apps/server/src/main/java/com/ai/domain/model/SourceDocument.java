package com.ai.domain.model;

public record SourceDocument(
    int index,
    String text,
    double score,
    String documentTitle,
    java.util.Map<String, Object> metadata
) {
    public SourceDocument {
        if (index < 1) throw new IllegalArgumentException("Source index must be at least 1");
        if (text == null) throw new IllegalArgumentException("Source text cannot be null");
        if (documentTitle == null) throw new IllegalArgumentException("Document title cannot be null");
        if (score < 0.0 || score > 1.0) throw new IllegalArgumentException("Score must be between 0.0 and 1.0, got: " + score);
    }
}
