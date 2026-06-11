package com.ai.domain.model;

public record SourceDocument(
    String text,
    double score,
    java.util.Map<String, Object> metadata
) {}
