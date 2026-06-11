package com.ai.application.usecase;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UploadDocumentUseCase {
    private static final Logger log = LoggerFactory.getLogger(UploadDocumentUseCase.class);
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    private final DocumentRepositoryPort documentRepository;
    private final EmbeddingPort embeddingPort;
    private final VectorSearchPort vectorSearchPort;

    public UploadDocumentUseCase(DocumentRepositoryPort documentRepository, 
                                  EmbeddingPort embeddingPort,
                                  VectorSearchPort vectorSearchPort) {
        this.documentRepository = documentRepository;
        this.embeddingPort = embeddingPort;
        this.vectorSearchPort = vectorSearchPort;
    }

    public Document execute(String title, String fileName, Long fileSize, String content) {
        log.info("Uploading document: {}", title);
        
        Document document = new Document(UUID.randomUUID(), title, fileName, fileSize);
        document.markProcessing();
        document = documentRepository.save(document);

        try {
            List<String> chunks = chunkText(content);
            log.info("Split into {} chunks", chunks.size());

            List<DocumentChunk> embeddedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                float[] embedding = embeddingPort.embed(chunkText);
                
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", title);
                metadata.put("fileName", fileName);
                
                DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID(),
                    document.getId(),
                    chunkText,
                    i,
                    metadata
                ).withEmbedding(embedding);
                
                try {
                    vectorSearchPort.saveChunk(chunk);
                    embeddedChunks.add(chunk);
                    log.debug("Chunk {}/{} saved successfully for document {}", i + 1, chunks.size(), document.getId());
                } catch (Exception e) {
                    log.error("Failed to save chunk {} for document {}: {}", i, document.getId(), e.getMessage());
                    throw e;
                }
            }
            
            log.info("All {} chunks saved to vector store for document {}", embeddedChunks.size(), document.getId());

            document.markReady();
            document = documentRepository.save(document);
            log.info("Document uploaded successfully: {}", document.getId());
            return document;
            
        } catch (Exception e) {
            log.error("Failed to process document", e);
            document.markFailed();
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?。！？])\\s*");
        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;

        for (String sentence : sentences) {
            int sentenceLength = sentence.length();
            if (currentLength + sentenceLength > CHUNK_SIZE && currentLength > 0) {
                chunks.add(currentChunk.toString().trim());
                String overlap = currentChunk.toString();
                currentChunk = new StringBuilder(
                    overlap.length() > CHUNK_OVERLAP 
                        ? overlap.substring(overlap.length() - CHUNK_OVERLAP) 
                        : overlap
                );
                currentLength = currentChunk.length();
            }
            currentChunk.append(sentence).append(" ");
            currentLength += sentenceLength + 1;
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }
}
