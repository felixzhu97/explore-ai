package com.ai.rag.model;

import jakarta.validation.constraints.NotBlank;

public record RagChatRequest(
		@NotBlank String query,
		String session_id,
		Integer top_k,
		Double temperature,
		String[] doc_ids
) {
	public int topK() {
		return top_k != null && top_k > 0 ? top_k : 5;
	}

	public double temperature() {
		return temperature != null ? temperature : 0.7;
	}
}
