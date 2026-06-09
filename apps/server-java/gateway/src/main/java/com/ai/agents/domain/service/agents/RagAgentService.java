package com.ai.agents.domain.service.agents;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG Agent domain service.
 * Manages document retrieval and knowledge base operations.
 */
@Service
public final class RagAgentService {

    private final Map<String, Document> documents = new HashMap<>();
    private final Map<String, List<String>> documentChunks = new HashMap<>();

    /**
     * Index a document.
     */
    public Document indexDocument(String content, String title, Map<String, String> metadata) {
        String docId = UUID.randomUUID().toString();
        Document doc = new Document(
                docId,
                title,
                content,
                metadata != null ? metadata : Map.of(),
                java.time.Instant.now(),
                List.of()
        );

        List<String> chunks = chunkContent(content, 500);
        documentChunks.put(docId, chunks);

        Document indexedDoc = new Document(
                docId,
                title,
                content,
                metadata,
                doc.createdAt(),
                chunks
        );

        documents.put(docId, indexedDoc);
        return indexedDoc;
    }

    private List<String> chunkContent(String content, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = content.split("[.。!！?？\\n]+");

        StringBuilder currentChunk = new StringBuilder();
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(". ");
        }
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }

    /**
     * Search documents.
     */
    public List<SearchResult> search(String query, int topK) {
        List<SearchResult> results = new ArrayList<>();

        for (Document doc : documents.values()) {
            List<String> chunks = documentChunks.get(doc.id());
            if (chunks == null) continue;

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                double score = calculateRelevance(query, chunk);
                if (score > 0.1) {
                    results.add(new SearchResult(
                            doc.id(),
                            doc.title(),
                            chunk,
                            score,
                            i
                    ));
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.score(), a.score()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    private double calculateRelevance(String query, String content) {
        String[] queryTerms = query.toLowerCase().split("\\s+");
        String contentLower = content.toLowerCase();

        int matches = 0;
        for (String term : queryTerms) {
            if (contentLower.contains(term)) {
                matches++;
            }
        }
        return (double) matches / queryTerms.length;
    }

    /**
     * Get document by ID.
     */
    public Optional<Document> getDocument(String docId) {
        return Optional.ofNullable(documents.get(docId));
    }

    /**
     * List documents.
     */
    public List<Document> listDocuments() {
        return new ArrayList<>(documents.values());
    }

    /**
     * Delete document.
     */
    public boolean deleteDocument(String docId) {
        documentChunks.remove(docId);
        return documents.remove(docId) != null;
    }

    public record Document(
            String id,
            String title,
            String content,
            Map<String, String> metadata,
            java.time.Instant createdAt,
            List<String> chunks
    ) {
        public String idValue() { return id; }
        public int chunkCount() { return chunks != null ? chunks.size() : 0; }
    }

    public record SearchResult(
            String docId,
            String title,
            String chunk,
            double score,
            int chunkIndex
    ) {}
}
