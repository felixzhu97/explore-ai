package com.ai.rag.service;

import com.ai.rag.exception.RagException;
import com.ai.rag.model.Document;
import com.ai.rag.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final VectorSearchService vectorSearchService;

	public DocumentService(DocumentRepository documentRepository, VectorSearchService vectorSearchService) {
		this.documentRepository = documentRepository;
		this.vectorSearchService = vectorSearchService;
	}

	public Document upload(MultipartFile file) {
		String filename = file.getOriginalFilename();
		if (filename == null || filename.isBlank()) {
			throw new RagException("Filename must not be blank");
		}

		String contentType = file.getContentType();
		Long size = file.getSize();

		List<String> chunks;
		try {
			String content = new String(file.getBytes());
			chunks = chunkText(content, 512, 128);
		}
		catch (IOException e) {
			throw new RagException("Failed to read uploaded file", e);
		}

		Document doc = documentRepository.save(filename, contentType, size, chunks);
		vectorSearchService.addSegments(chunks);
		return doc;
	}

	public List<Document> findAll(int page, int size) {
		return documentRepository.findAll(page, size);
	}

	public Document findById(UUID id) {
		return documentRepository.findById(id);
	}

	public void delete(UUID id) {
		documentRepository.deleteById(id);
	}

	private List<String> chunkText(String text, int chunkSize, int overlap) {
		if (text == null || text.isEmpty()) {
			return List.of();
		}

		List<String> chunks = new ArrayList<>();
		int start = 0;

		while (start < text.length()) {
			int end = Math.min(start + chunkSize, text.length());
			chunks.add(text.substring(start, end));
			start += chunkSize - overlap;
			if (start >= text.length()) break;
		}

		return chunks;
	}
}
