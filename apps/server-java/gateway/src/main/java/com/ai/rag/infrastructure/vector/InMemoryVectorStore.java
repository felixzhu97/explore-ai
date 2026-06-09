package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.DocumentId;
import com.ai.rag.domain.SourceDocument;
import com.ai.rag.domain.VectorStore;

import java.util.*;

/**
 * In-memory vector store implementation for development/testing.
 * Production should use QdrantVectorStore.
 */
public class InMemoryVectorStore implements VectorStore {

    private final Map<String, VectorEntry> vectors = new HashMap<>();
    private final Map<String, List<String>> docChunks = new HashMap<>();

    @Override
    public void addSegments(List<String> chunks, DocumentId docId, String filename) {
        docChunks.put(docId.toString(), chunks);
        // In-memory storage - just store the chunks
        for (int i = 0; i < chunks.size(); i++) {
            vectors.put(docId.toString() + "_" + i, new VectorEntry(chunks.get(i), docId.toString(), filename));
        }
    }

    @Override
    public List<String> searchSimilar(String query, int topK) {
        // Simple text matching for in-memory implementation
        List<Map.Entry<String, VectorEntry>> entries = new ArrayList<>(vectors.entrySet());
        entries.sort((a, b) -> {
            double scoreA = calculateSimilarity(query, a.getValue().text);
            double scoreB = calculateSimilarity(query, b.getValue().text);
            return Double.compare(scoreB, scoreA);
        });
        
        return entries.stream()
                .limit(topK)
                .map(e -> e.getValue().text)
                .toList();
    }

    @Override
    public List<String> searchSimilar(String query, List<String> docIds, int topK) {
        if (docIds == null || docIds.isEmpty()) {
            return searchSimilar(query, topK);
        }
        
        Set<String> docIdSet = new HashSet<>(docIds);
        return vectors.entrySet().stream()
                .filter(e -> docIdSet.contains(e.getValue().docId))
                .sorted((a, b) -> {
                    double scoreA = calculateSimilarity(query, a.getValue().text);
                    double scoreB = calculateSimilarity(query, b.getValue().text);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(topK)
                .map(e -> e.getValue().text)
                .toList();
    }

    @Override
    public List<SourceDocument> searchWithScores(String query, int topK) {
        List<Map.Entry<String, VectorEntry>> entries = new ArrayList<>(vectors.entrySet());
        entries.sort((a, b) -> {
            double scoreA = calculateSimilarity(query, a.getValue().text);
            double scoreB = calculateSimilarity(query, b.getValue().text);
            return Double.compare(scoreB, scoreA);
        });
        
        return entries.stream()
                .limit(topK)
                .map(e -> SourceDocument.of(e.getValue().text, calculateSimilarity(query, e.getValue().text)))
                .toList();
    }

    @Override
    public List<SourceDocument> searchWithScores(String query, List<String> docIds, int topK) {
        if (docIds == null || docIds.isEmpty()) {
            return searchWithScores(query, topK);
        }
        
        Set<String> docIdSet = new HashSet<>(docIds);
        return vectors.entrySet().stream()
                .filter(e -> docIdSet.contains(e.getValue().docId))
                .sorted((a, b) -> {
                    double scoreA = calculateSimilarity(query, a.getValue().text);
                    double scoreB = calculateSimilarity(query, b.getValue().text);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(topK)
                .map(e -> SourceDocument.of(e.getValue().text, calculateSimilarity(query, e.getValue().text)))
                .toList();
    }

    @Override
    public void deleteByDocId(DocumentId docId) {
        String prefix = docId.toString();
        vectors.keySet().removeIf(key -> key.startsWith(prefix));
        docChunks.remove(docId.toString());
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("type", "in-memory");
        stats.put("total_vectors", vectors.size());
        stats.put("total_documents", docChunks.size());
        return stats;
    }

    private double calculateSimilarity(String query, String text) {
        // Simple word-based similarity for in-memory implementation
        Set<String> queryWords = new HashSet<>(Arrays.asList(query.toLowerCase().split("\\s+")));
        Set<String> textWords = new HashSet<>(Arrays.asList(text.toLowerCase().split("\\s+")));
        
        Set<String> intersection = new HashSet<>(queryWords);
        intersection.retainAll(textWords);
        
        if (intersection.isEmpty()) return 0.0;
        return (double) intersection.size() / queryWords.size();
    }

    private record VectorEntry(String text, String docId, String filename) {}
}
