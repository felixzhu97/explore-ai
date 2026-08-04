package com.ai.rag.domain;

public record SourceDocument(
    String text,
    double score,
    java.util.Map<String, Object> metadata
) {}
