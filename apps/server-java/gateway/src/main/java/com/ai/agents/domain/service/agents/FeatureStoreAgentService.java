package com.ai.agents.domain.service.agents;

import com.ai.agents.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Feature Store Agent domain service.
 * Manages feature definitions, materialization, and vector operations.
 */
@Service
public final class FeatureStoreAgentService {

    private final Map<String, FeatureVector> featureVectors = new HashMap<>();
    private final Map<String, FeatureDefinition> featureDefinitions = new HashMap<>();

    /**
     * Create a feature vector.
     */
    public FeatureVector createVector(String featureName, String entityId, Map<String, Object> features) {
        FeatureVector vector = FeatureVector.create(featureName, entityId, features);
        String key = featureName + ":" + entityId;
        featureVectors.put(key, vector);
        return vector;
    }

    /**
     * Get feature vector.
     */
    public Optional<FeatureVector> getVector(String featureName, String entityId) {
        return Optional.ofNullable(featureVectors.get(featureName + ":" + entityId));
    }

    /**
     * List feature vectors.
     */
    public List<FeatureVector> listVectors(String featureName) {
        return featureVectors.values().stream()
                .filter(v -> featureName == null || featureName.equals(v.featureName()))
                .toList();
    }

    /**
     * Register a feature definition.
     */
    public FeatureDefinition registerFeature(String name, String entityType, String dataType, String description) {
        FeatureDefinition definition = new FeatureDefinition(
                name,
                entityType,
                dataType,
                description,
                java.time.Instant.now()
        );
        featureDefinitions.put(name, definition);
        return definition;
    }

    /**
     * Get feature definition.
     */
    public Optional<FeatureDefinition> getFeature(String name) {
        return Optional.ofNullable(featureDefinitions.get(name));
    }

    /**
     * List feature definitions.
     */
    public List<FeatureDefinition> listFeatures(String entityType) {
        return featureDefinitions.values().stream()
                .filter(f -> entityType == null || entityType.equals(f.entityType()))
                .toList();
    }

    /**
     * Materialize features for an entity.
     */
    public MaterializationResult materialize(String featureName, String entityId) {
        List<FeatureVector> vectors = listVectors(featureName);
        long count = vectors.stream()
                .filter(v -> v.entityId().equals(entityId))
                .count();

        return new MaterializationResult(
                featureName,
                entityId,
                count,
                java.time.Instant.now(),
                "completed"
        );
    }

    public record FeatureDefinition(
            String name,
            String entityType,
            String dataType,
            String description,
            java.time.Instant createdAt
    ) {}

    public record MaterializationResult(
            String featureName,
            String entityId,
            long recordCount,
            java.time.Instant timestamp,
            String status
    ) {}
}
