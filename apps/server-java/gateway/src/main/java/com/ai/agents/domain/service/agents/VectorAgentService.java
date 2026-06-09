package com.ai.agents.domain.service.agents;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Vector DB Agent domain service.
 * Manages ChromaDB/Qdrant vector operations.
 */
@Service
public final class VectorAgentService {

    private final Map<String, Collection> collections = new HashMap<>();
    private final Map<String, List<VectorEntry>> vectors = new HashMap<>();

    /**
     * Create a collection.
     */
    public Collection createCollection(String name, int dimension, String description) {
        Collection collection = new Collection(
                name,
                dimension,
                description,
                0,
                Instant.now()
        );
        collections.put(name, collection);
        vectors.put(name, new ArrayList<>());
        return collection;
    }

    /**
     * Get collection.
     */
    public Optional<Collection> getCollection(String name) {
        return Optional.ofNullable(collections.get(name));
    }

    /**
     * List collections.
     */
    public List<Collection> listCollections() {
        return new ArrayList<>(collections.values());
    }

    /**
     * Add vectors to collection.
     */
    public List<String> addVectors(String collectionName, List<String> ids, List<float[]> embeddings, List<String> documents, Map<String, String> metadata) {
        List<String> resultIds = new ArrayList<>();
        List<VectorEntry> entries = vectors.computeIfAbsent(collectionName, k -> new ArrayList<>());

        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            float[] embedding = embeddings.get(i);
            String document = documents != null && i < documents.size() ? documents.get(i) : "";
            Map<String, String> meta = metadata != null ? metadata : Map.of();

            entries.add(new VectorEntry(id, embedding, document, meta, Instant.now()));
            resultIds.add(id);
        }

        Collection collection = collections.get(collectionName);
        if (collection != null) {
            collections.put(collectionName, new Collection(
                    collection.name(),
                    collection.dimension(),
                    collection.description(),
                    entries.size(),
                    collection.createdAt()
            ));
        }

        vectors.put(collectionName, entries);
        return resultIds;
    }

    /**
     * Query vectors.
     */
    public List<QueryResult> query(String collectionName, float[] queryEmbedding, int topK) {
        List<VectorEntry> entries = vectors.getOrDefault(collectionName, List.of());

        List<QueryResult> results = new ArrayList<>();
        for (VectorEntry entry : entries) {
            double similarity = calculateCosineSimilarity(queryEmbedding, entry.embedding());
            results.add(new QueryResult(
                    entry.id(),
                    entry.document(),
                    similarity,
                    entry.metadata()
            ));
        }

        results.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
        return results.subList(0, Math.min(topK, results.size()));
    }

    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Delete vectors.
     */
    public boolean deleteVectors(String collectionName, List<String> ids) {
        List<VectorEntry> entries = vectors.get(collectionName);
        if (entries == null) return false;

        int initialSize = entries.size();
        entries.removeIf(e -> ids.contains(e.id()));
        int deleted = initialSize - entries.size();

        Collection collection = collections.get(collectionName);
        if (collection != null) {
            collections.put(collectionName, new Collection(
                    collection.name(),
                    collection.dimension(),
                    collection.description(),
                    entries.size(),
                    collection.createdAt()
            ));
        }

        return deleted > 0;
    }

    public record Collection(
            String name,
            int dimension,
            String description,
            int count,
            Instant createdAt
    ) {}

    public record VectorEntry(
            String id,
            float[] embedding,
            String document,
            Map<String, String> metadata,
            Instant createdAt
    ) {}

    public record QueryResult(
            String id,
            String document,
            double similarity,
            Map<String, String> metadata
    ) {}
}
