package com.ai.rag.repository;

import com.ai.rag.exception.RagException;
import com.ai.rag.model.Document;
import com.qdrant.client.QdrantClient;
import com.qdrant.client.grpc.Points;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class DocumentRepository {

	private static final String COLLECTION = "documents";

	private final QdrantClient qdrantClient;

	public DocumentRepository(QdrantClient qdrantClient) {
		this.qdrantClient = qdrantClient;
	}

	public Document save(String filename, String contentType, Long size, List<String> chunks) {
		String documentId = UUID.randomUUID().toString();
		LocalDateTime createdAt = LocalDateTime.now();

		return new Document(
				UUID.fromString(documentId),
				filename,
				contentType,
				size,
				chunks.size(),
				createdAt
		);
	}

	public List<Document> findAll(int page, int size) {
		return List.of();
	}

	public Document findById(UUID id) {
		return null;
	}

	public void deleteById(UUID id) {
	}
}
