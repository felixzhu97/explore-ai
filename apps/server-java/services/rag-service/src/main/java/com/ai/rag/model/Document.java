package com.ai.rag.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record Document(
		UUID id,
		String filename,
		String contentType,
		Long size,
		Integer chunkCount,
		LocalDateTime createdAt
) {

	public Document withFilename(String filename) {
		return new Document(this.id, filename, this.contentType, this.size, this.chunkCount, this.createdAt);
	}
}
