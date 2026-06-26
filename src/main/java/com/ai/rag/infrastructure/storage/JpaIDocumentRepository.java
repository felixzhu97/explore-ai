package com.ai.rag.infrastructure.storage;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.repository.IDocumentRepository;
import com.ai.rag.domain.vo.DocumentId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Document repository using Spring Data JPA.
 */
@Component
public class JpaIDocumentRepository implements IDocumentRepository {

    private final SpringDataDocumentRepository documentRepository;

    public JpaIDocumentRepository(SpringDataDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional
    public Document save(Document document) {
        DocumentEntity entity = toEntity(document);
        DocumentEntity saved = documentRepository.save(entity);
        return toDomain(saved);
    }

    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return documentRepository.findById(id).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return documentRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id) {
        documentRepository.deleteById(id);
    }

    private DocumentEntity toEntity(Document document) {
        return new DocumentEntity(
                document.getId().value(),
                document.getTitle(),
                document.getFileName(),
                document.getFileSize(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private Document toDomain(DocumentEntity entity) {
        return new Document(
                DocumentId.of(entity.getId()),
                entity.getTitle(),
                entity.getFileName(),
                entity.getFileSize(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
