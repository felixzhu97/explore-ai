package com.ai.agents.domain.service.agents;

import com.ai.agents.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Model Agent domain service.
 * Manages ML model registry, versioning, and rollback.
 */
@Service
public final class ModelAgentService {

    private final Map<String, ModelVersion> models = new HashMap<>();

    /**
     * Register a new model.
     */
    public ModelVersion register(String modelName, String version, String artifactUri) {
        ModelVersion model = ModelVersion.register(modelName, version, artifactUri);
        String key = modelName + ":" + version;
        models.put(key, model);
        return model;
    }

    /**
     * Get model version.
     */
    public Optional<ModelVersion> getModel(String modelName, String version) {
        return Optional.ofNullable(models.get(modelName + ":" + version));
    }

    /**
     * List model versions.
     */
    public List<ModelVersion> listModels(String modelName) {
        return models.values().stream()
                .filter(m -> modelName == null || modelName.equals(m.modelName()))
                .toList();
    }

    /**
     * Deploy model to staging.
     */
    public ModelVersion deployToStaging(String modelName, String version) {
        return updateStatus(modelName, version, ModelVersion.ModelStatus.STAGING);
    }

    /**
     * Deploy model to production.
     */
    public ModelVersion deployToProduction(String modelName, String version) {
        return updateStatus(modelName, version, ModelVersion.ModelStatus.PRODUCTION);
    }

    /**
     * Deprecate model version.
     */
    public ModelVersion deprecate(String modelName, String version) {
        return updateStatus(modelName, version, ModelVersion.ModelStatus.DEPRECATED);
    }

    /**
     * Archive model version.
     */
    public ModelVersion archive(String modelName, String version) {
        return updateStatus(modelName, version, ModelVersion.ModelStatus.ARCHIVED);
    }

    private ModelVersion updateStatus(String modelName, String version, ModelVersion.ModelStatus newStatus) {
        String key = modelName + ":" + version;
        ModelVersion model = models.get(key);
        if (model == null) {
            throw new IllegalArgumentException("Model not found: " + key);
        }
        ModelVersion updated = model.stage(newStatus);
        models.put(key, updated);
        return updated;
    }

    /**
     * Rollback from production to previous version.
     */
    public RollbackResult rollback(String modelName) {
        List<ModelVersion> versions = listModels(modelName);

        ModelVersion currentProd = versions.stream()
                .filter(m -> m.isProduction())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No production version"));

        ModelVersion rollbackTarget = versions.stream()
                .filter(m -> m.status() == ModelVersion.ModelStatus.STAGING)
                .findFirst()
                .orElse(versions.stream()
                        .filter(m -> m.status() == ModelVersion.ModelStatus.REGISTERED)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No version to rollback to")));

        archive(modelName, currentProd.version());
        deployToProduction(modelName, rollbackTarget.version());

        return new RollbackResult(
                modelName,
                currentProd.version(),
                rollbackTarget.version(),
                "Rollback successful"
        );
    }

    public record RollbackResult(
            String modelName,
            String fromVersion,
            String toVersion,
            String message
    ) {}
}
