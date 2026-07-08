package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spring AI VectorStore adapter backed by H2 document_chunks storage.
 */
@Component
public class H2DocumentVectorStore implements VectorStore {

    private final H2VectorAdapter vectorAdapter;
    private final EmbeddingAdapter embeddingAdapter;

    public H2DocumentVectorStore(H2VectorAdapter vectorAdapter, EmbeddingAdapter embeddingAdapter) {
        this.vectorAdapter = vectorAdapter;
        this.embeddingAdapter = embeddingAdapter;
    }

    @Override
    public void add(List<Document> documents) {
        for (Document document : documents) {
            DocumentChunk chunk = toChunk(document);
            vectorAdapter.saveChunk(chunk);
        }
    }

    @Override
    public void delete(List<String> idList) {
        for (String id : idList) {
            vectorAdapter.deleteChunksByDocumentId(DocumentId.of(UUID.fromString(id)));
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        throw new UnsupportedOperationException("Filter-based delete not supported on H2 vector store");
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        float[] queryEmbedding = embeddingAdapter.embed(request.getQuery());
        List<DocumentChunk> chunks = vectorAdapter.search(queryEmbedding, request.getTopK());
        return chunks.stream().map(this::toDocument).toList();
    }

    private DocumentChunk toChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        UUID documentId = UUID.fromString(String.valueOf(metadata.getOrDefault("document_id", document.getId())));
        int chunkIndex = ((Number) metadata.getOrDefault("chunk_index", 0)).intValue();
        float[] embedding = embeddingAdapter.embed(document.getText());
        return DocumentChunk.reconstitute(
                DocumentId.of(UUID.fromString(document.getId())),
                DocumentId.of(documentId),
                document.getText(),
                chunkIndex,
                metadata,
                embedding,
                Instant.now());
    }

    private Document toDocument(DocumentChunk chunk) {
        Map<String, Object> metadata = chunk.getMetadata() != null ? chunk.getMetadata() : Map.of();
        return Document.builder()
                .id(chunk.getId().value().toString())
                .text(chunk.getContent())
                .metadata(metadata)
                .build();
    }
}
