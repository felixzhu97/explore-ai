package com.ai.application.usecase;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.domain.exception.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class DeleteDocumentUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteDocumentUseCase.class);

    private final DocumentRepositoryPort documentRepository;

    public DeleteDocumentUseCase(DocumentRepositoryPort documentRepository) {
        this.documentRepository = documentRepository;
    }

    public void execute(UUID documentId) {
        log.info("Deleting document: {}", documentId);
        
        documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
        
        documentRepository.deleteChunksByDocumentId(documentId);
        documentRepository.delete(documentId);
        
        log.info("Document deleted successfully: {}", documentId);
    }
}
