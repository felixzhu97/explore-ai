package com.ai.rag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorSearchService {

	private final EmbeddingModel embeddingModel;
	private final EmbeddingStore<TextSegment> embeddingStore;

	public VectorSearchService(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		this.embeddingStore = new InMemoryEmbeddingStore<>();
	}

	public List<String> searchSimilar(String query, int topK) {
		Embedding embedding = embeddingModel.embed(query);

		EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
				.queryEmbedding(embedding)
				.maxResults(topK)
				.build();

		EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
		return result.matches().stream()
				.map(match -> match.embedded().text())
				.toList();
	}

	public void addSegments(List<String> chunks) {
		if (chunks == null || chunks.isEmpty()) {
			return;
		}

		List<TextSegment> segments = chunks.stream()
				.map(TextSegment::from)
				.toList();

		List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

		for (int i = 0; i < segments.size(); i++) {
			embeddingStore.add(embeddings.get(i), segments.get(i));
		}
	}
}
