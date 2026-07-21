package com.ai.workflow.domain.model;

import java.util.List;

/**
 * Orchestrator analysis, planned tasks, parallel worker outputs, and synthesis.
 */
public record OrchestratorWorkersResult(
        String analysis,
        List<WorkerTask> tasks,
        List<String> workerResponses,
        String synthesis) {

    public OrchestratorWorkersResult {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        workerResponses = workerResponses == null ? List.of() : List.copyOf(workerResponses);
    }
}
