package com.ai.rag.model;

public record SourceDocument(
		String text,
		double score
) {
}
