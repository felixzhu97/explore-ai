package com.ai.rag.controller;

import com.ai.rag.model.Document;
import com.ai.rag.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rag/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	public record DocumentListResponse(List<DocItem> documents) {
		public record DocItem(String doc_id, String filename) {}
	}

	public record UploadResponse(String id) {}

	@GetMapping("/")
	public Mono<DocumentListResponse> list() {
		return Mono.fromCallable(() -> {
			List<Document> docs = documentService.findAll(0, 100);
			return new DocumentListResponse(
					docs.stream()
							.map(d -> new DocumentListResponse.DocItem(d.id().toString(), d.filename()))
							.collect(Collectors.toList())
			);
		});
	}

	@PostMapping("/upload")
	public Mono<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
		return Mono.fromCallable(() -> {
			Document doc = documentService.upload(file);
			return new UploadResponse(doc.id().toString());
		});
	}

	@DeleteMapping("/{docId}")
	public Mono<Void> delete(@PathVariable String docId) {
		return Mono.fromRunnable(() -> documentService.delete(UUID.fromString(docId)));
	}

	@GetMapping("/{docId}")
	public Mono<Document> get(@PathVariable String docId) {
		return Mono.fromCallable(() -> documentService.findById(UUID.fromString(docId)));
	}
}
