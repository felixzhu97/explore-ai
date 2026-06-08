package com.ai.rag.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import com.qdrant.client.QdrantClient;
import com.qdrant.client.grpc.Points;
import com.qdrant.client.grpc.JsonWithString;
import io.qdrant.client.grpc.Points.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Qdrant implementation of LangChain4j EmbeddingStore.
 * Provides production-ready vector storage with metadata support.
 */
public class QdrantEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final Logger log = LoggerFactory.getLogger(QdrantEmbeddingStore.class);

    private final QdrantClient qdrantClient;
    private final String collectionName;
    private final int dimension;
    private volatile boolean initialized = false;

    public QdrantEmbeddingStore(
            QdrantClient qdrantClient,
            String collectionName,
            int dimension
    ) {
        this.qdrantClient = qdrantClient;
        this.collectionName = collectionName;
        this.dimension = dimension;
    }

    /**
     * Initialize the collection if it doesn't exist.
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            try {
                boolean exists = qdrantClient.collectionExists(collectionName);
                if (!exists) {
                    qdrantClient.createCollection(
                            collectionName,
                            Points.VectorParams.newBuilder()
                                    .setSize(dimension)
                                    .setDistance(Points.Distance.Cosine)
                                    .build()
                    );
                    log.info("Created Qdrant collection: {}", collectionName);
                }
                initialized = true;
            } catch (Exception e) {
                log.error("Failed to initialize Qdrant collection: {}", collectionName, e);
                throw new RuntimeException("Failed to initialize Qdrant collection", e);
            }
        }
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, segment);
        return id;
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, null);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        if (embeddings.size() != segments.size()) {
            throw new IllegalArgumentException("The number of embeddings must match the number of segments");
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            ids.add(id);
            addInternal(id, embeddings.get(i), segments.get(i));
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            String id = UUID.randomUUID().toString();
            ids.add(id);
            addInternal(id, embedding, null);
        }
        return ids;
    }

    private void addInternal(String id, Embedding embedding, TextSegment segment) {
        initialize();

        Map<String, String> payload = new HashMap<>();
        if (segment != null) {
            payload.put("text", segment.text());
            if (segment.metadata() != null) {
                segment.metadata().asMap().forEach((key, value) -> {
                    if (value != null) {
                        payload.put(key, String.valueOf(value));
                    }
                });
            }
        }
        payload.put("created_at", Instant.now().toString());

        PointStruct point = PointStruct.newBuilder()
                .setId(PointId.newBuilder().setUuid(id).build())
                .setVectors(embedding.vectorAsList().stream()
                        .map(Float::doubleValue)
                        .map(d -> d)
                        .collect(Collectors.toList()))
                .setPayload(convertToJsonPayload(payload))
                .build();

        qdrantClient.upsert(collectionName, Collections.singletonList(point));
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        initialize();

        try {
            SearchPoints searchRequest = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setLimit(request.maxResults())
                    .addAllVector(request.queryEmbedding().vectorAsList())
                    .setWithPayload(WithPayloadSelector.newBuilder()
                            .setEnable(true)
                            .build())
                    .build();

            List<ScoredPoint> results = qdrantClient.searchPoints(searchRequest);

            List<EmbeddingMatch<TextSegment>> matches = results.stream()
                    .map(this::toEmbeddingMatch)
                    .collect(Collectors.toList());

            return new EmbeddingSearchResult<>(matches);

        } catch (Exception e) {
            log.error("Qdrant search failed", e);
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }
    }

    /**
     * Search with optional metadata filtering by doc_id.
     */
    public EmbeddingSearchResult<TextSegment> search(String query, List<String> docIds, int topK) {
        initialize();

        try {
            SearchPoints.Builder builder = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setLimit(topK)
                    .addAllVector(query)
                    .setWithPayload(WithPayloadSelector.newBuilder()
                            .setEnable(true)
                            .build());

            if (docIds != null && !docIds.isEmpty()) {
                builder.setFilter(Filter.newBuilder()
                        .addMust(FieldCondition.newBuilder()
                                .setKey("doc_id")
                                .setMatch(Match.newBuilder()
                                        .setAny(com.google.protobuf.Struct.newBuilder()
                                                .putAllFields(docIds.stream()
                                                        .collect(Collectors.toMap(
                                                                id -> "v_" + id,
                                                                id -> com.google.protobuf.Value.newBuilder()
                                                                        .setStringValue(id)
                                                                        .build()
                                                        )))
                                                .build())
                                        .build())
                                .build())
                        .build());
            }

            List<ScoredPoint> results = qdrantClient.searchPoints(builder.build());
            List<EmbeddingMatch<TextSegment>> matches = results.stream()
                    .map(this::toEmbeddingMatch)
                    .collect(Collectors.toList());

            return new EmbeddingSearchResult<>(matches);

        } catch (Exception e) {
            log.error("Qdrant filtered search failed", e);
            return new EmbeddingSearchResult<>(Collections.emptyList());
        }
    }

    private EmbeddingMatch<TextSegment> toEmbeddingMatch(ScoredPoint point) {
        float[] vector = new float[point.getVectorsList().size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) point.getVectorsList().get(i);
        }

        TextSegment segment = null;
        if (point.hasPayload()) {
            String text = "";
            Map<String, Object> metadata = new HashMap<>();

            Map<String, com.google.protobuf.Value> fieldsMap = point.getPayload().getFieldsMap();
            for (Map.Entry<String, com.google.protobuf.Value> entry : fieldsMap.entrySet()) {
                String key = entry.getKey();
                com.google.protobuf.Value value = entry.getValue();
                if ("text".equals(key)) {
                    text = value.getStringValue();
                } else {
                    if (value.hasStringValue()) {
                        metadata.put(key, value.getStringValue());
                    } else if (value.hasNumberValue()) {
                        metadata.put(key, value.getNumberValue());
                    } else if (value.hasBoolValue()) {
                        metadata.put(key, value.getBoolValue());
                    }
                }
            }

            if (!text.isEmpty() || !metadata.isEmpty()) {
                segment = TextSegment.from(text, new dev.langchain4j.data.document.Metadata(metadata));
            }
        }

        return new EmbeddingMatch<>(
                (double) point.getScore(),
                String.valueOf(point.getId()),
                new Embedding(vector),
                segment
        );
    }

    private JsonWithString convertToJsonPayload(Map<String, String> payload) {
        com.google.protobuf.Struct.Builder structBuilder = com.google.protobuf.Struct.newBuilder();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            structBuilder.putFields(entry.getKey(),
                    com.google.protobuf.Value.newBuilder()
                            .setStringValue(entry.getValue())
                            .build());
        }
        return JsonWithString.newBuilder()
                .setJson(structBuilder.build().toString())
                .build();
    }

    /**
     * Delete all points for a specific document.
     */
    public void deleteByDocId(String docId) {
        initialize();

        try {
            Filter filter = Filter.newBuilder()
                    .addMust(FieldCondition.newBuilder()
                            .setKey("doc_id")
                            .setMatch(Match.newBuilder()
                                    .setKeyword(com.google.protobuf.Value.newBuilder()
                                            .setStringValue(docId)
                                            .build())
                                    .build())
                            .build())
                    .build();

            qdrantClient.delete(
                    collectionName,
                    Points.Filter.newBuilder()
                            .addAllMust(filter.getMustList())
                            .build(),
                    true
            );

            log.info("Deleted vectors for doc_id: {}", docId);

        } catch (Exception e) {
            log.error("Failed to delete vectors for doc_id: {}", docId, e);
        }
    }

    /**
     * Delete all points from the collection.
     */
    public void deleteAll() {
        initialize();

        try {
            qdrantClient.delete(collectionName, null, true);
            log.info("Deleted all vectors from collection: {}", collectionName);
        } catch (Exception e) {
            log.error("Failed to delete all vectors", e);
        }
    }

    /**
     * Get collection info/stats.
     */
    public Map<String, Object> getStats() {
        initialize();

        try {
            CollectionInfo info = qdrantClient.getCollectionInfo(collectionName);
            Map<String, Object> stats = new HashMap<>();
            stats.put("points_count", info.getResult().getPointsCount());
            stats.put("vectors_count", info.getResult().getVectorsCount());
            stats.put("indexed_vectors_count", info.getResult().getIndexedVectorsCount());
            stats.put("collection_name", collectionName);
            stats.put("dimension", dimension);
            return stats;
        } catch (Exception e) {
            log.error("Failed to get collection stats", e);
            return Map.of("error", e.getMessage());
        }
    }
}
