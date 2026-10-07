package com.ai.rag.domain.model;

public record SourceCitation(
    String content, double score, java.util.Map<String, Object> metadata) {}
