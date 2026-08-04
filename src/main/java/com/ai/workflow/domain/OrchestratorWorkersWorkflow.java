package com.ai.workflow.domain;

import com.ai.workflow.domain.OrchestratorWorkersResult;

/**
 * Orchestrator plans subtasks; workers run in parallel; synthesizer combines outputs.
 */
public interface OrchestratorWorkersWorkflow {

    OrchestratorWorkersResult process(String task);
}
