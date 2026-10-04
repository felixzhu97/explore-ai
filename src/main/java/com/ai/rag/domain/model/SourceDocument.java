package com.ai.rag.domain.model;

public record SourceDocument(
    String content, double score, java.util.Map<String, Object> metadata) {}
