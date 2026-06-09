package com.ai.agents.domain.service.agents;

import com.ai.agents.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Pipeline Agent domain service.
 * Manages DAG-based pipeline orchestration.
 */
@Service
public final class PipelineAgentService {

    private final Map<String, PipelineRun> pipelineRuns = new HashMap<>();

    /**
     * Create a new pipeline run.
     */
    public PipelineRun createRun(String pipelineName, List<String> steps) {
        PipelineRun run = PipelineRun.start(pipelineName, steps);
        pipelineRuns.put(run.id(), run);
        return run;
    }

    /**
     * Get pipeline run by ID.
     */
    public Optional<PipelineRun> getRun(String runId) {
        return Optional.ofNullable(pipelineRuns.get(runId));
    }

    /**
     * List pipeline runs.
     */
    public List<PipelineRun> listRuns(String pipelineName, PipelineRun.PipelineStatus status) {
        return pipelineRuns.values().stream()
                .filter(r -> pipelineName == null || pipelineName.equals(r.pipelineName()))
                .filter(r -> status == null || r.status() == status)
                .toList();
    }

    /**
     * Record step start.
     */
    public PipelineRun recordStepStart(String runId, String stepName) {
        PipelineRun run = pipelineRuns.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Pipeline run not found: " + runId);
        }
        PipelineRun updated = run.recordStepStart(stepName);
        pipelineRuns.put(runId, updated);
        return updated;
    }

    /**
     * Record step completion.
     */
    public PipelineRun recordStepComplete(String runId, String stepName, Object output) {
        PipelineRun run = pipelineRuns.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Pipeline run not found: " + runId);
        }
        PipelineRun updated = run.recordStepComplete(stepName, output);
        pipelineRuns.put(runId, updated);
        return updated;
    }

    /**
     * Record step failure.
     */
    public PipelineRun recordStepFailed(String runId, String stepName, String error) {
        PipelineRun run = pipelineRuns.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Pipeline run not found: " + runId);
        }
        PipelineRun updated = run.recordStepFailed(stepName, error);
        pipelineRuns.put(runId, updated);
        return updated;
    }

    /**
     * Complete the pipeline run.
     */
    public PipelineRun completeRun(String runId) {
        PipelineRun run = pipelineRuns.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Pipeline run not found: " + runId);
        }
        PipelineRun completed = run.complete();
        pipelineRuns.put(runId, completed);
        return completed;
    }

    /**
     * Cancel a running pipeline.
     */
    public PipelineRun cancelRun(String runId) {
        PipelineRun run = pipelineRuns.get(runId);
        if (run == null) {
            throw new IllegalArgumentException("Pipeline run not found: " + runId);
        }
        PipelineRun cancelled = run.cancel();
        pipelineRuns.put(runId, cancelled);
        return cancelled;
    }

    /**
     * Validate pipeline steps.
     */
    public boolean validateSteps(List<String> steps) {
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        for (String step : steps) {
            if (step == null || step.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
