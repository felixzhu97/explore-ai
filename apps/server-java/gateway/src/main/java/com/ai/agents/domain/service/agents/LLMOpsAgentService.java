package com.ai.agents.domain.service.agents;

import com.ai.agents.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * LLMOps Agent domain service.
 * Manages ML model lifecycle, training, and deployment.
 */
@Service
public final class LLMOpsAgentService {

    private final Map<String, ModelVersion> modelVersions = new HashMap<>();
    private final Map<String, ExperimentResult> experiments = new HashMap<>();

    /**
     * Register a new model version.
     */
    public ModelVersion registerModel(String modelName, String version, String artifactUri) {
        ModelVersion model = ModelVersion.register(modelName, version, artifactUri);
        String key = modelName + ":" + version;
        modelVersions.put(key, model);
        return model;
    }

    /**
     * Get model version.
     */
    public Optional<ModelVersion> getModel(String modelName, String version) {
        return Optional.ofNullable(modelVersions.get(modelName + ":" + version));
    }

    /**
     * List model versions.
     */
    public List<ModelVersion> listModels(String modelName, ModelVersion.ModelStatus status) {
        return modelVersions.values().stream()
                .filter(m -> modelName == null || modelName.equals(m.modelName()))
                .filter(m -> status == null || m.status() == status)
                .toList();
    }

    /**
     * Stage a model for testing.
     */
    public ModelVersion stageModel(String modelName, String version) {
        ModelVersion model = modelVersions.get(modelName + ":" + version);
        if (model == null) {
            throw new IllegalArgumentException("Model not found: " + modelName + ":" + version);
        }
        ModelVersion staged = model.stage(ModelVersion.ModelStatus.STAGING);
        modelVersions.put(modelName + ":" + version, staged);
        return staged;
    }

    /**
     * Promote model to production.
     */
    public ModelVersion promoteToProduction(String modelName, String version) {
        ModelVersion model = modelVersions.get(modelName + ":" + version);
        if (model == null) {
            throw new IllegalArgumentException("Model not found: " + modelName + ":" + version);
        }
        ModelVersion promoted = model.stage(ModelVersion.ModelStatus.PRODUCTION);
        modelVersions.put(modelName + ":" + version, promoted);
        return promoted;
    }

    /**
     * Rollback to previous version.
     */
    public ModelVersion rollback(String modelName) {
        List<ModelVersion> versions = listModels(modelName, null);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("No versions found for model: " + modelName);
        }

        ModelVersion current = versions.stream()
                .filter(m -> m.isProduction())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No production version to rollback from"));

        ModelVersion previous = versions.stream()
                .filter(m -> m.status() == ModelVersion.ModelStatus.STAGING)
                .findFirst()
                .orElse(versions.stream()
                        .filter(m -> m.status() == ModelVersion.ModelStatus.REGISTERED)
                        .findFirst()
                        .orElse(null));

        if (previous == null) {
            throw new IllegalStateException("No previous version to rollback to");
        }

        ModelVersion rolledBack = previous.stage(ModelVersion.ModelStatus.PRODUCTION);
        ModelVersion archived = current.archive();
        modelVersions.put(modelName + ":" + current.version(), archived);
        modelVersions.put(modelName + ":" + previous.version(), rolledBack);

        return rolledBack;
    }

    /**
     * Record experiment result.
     */
    public ExperimentResult recordExperiment(String experimentName, Map<String, Object> metrics, String status) {
        ExperimentResult result = new ExperimentResult(
                experimentName,
                metrics,
                status,
                java.time.Instant.now()
        );
        experiments.put(experimentName, result);
        return result;
    }

    public record ExperimentResult(
            String experimentName,
            Map<String, Object> metrics,
            String status,
            java.time.Instant timestamp
    ) {}
}
