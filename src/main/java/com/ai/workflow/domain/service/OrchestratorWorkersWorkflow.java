package com.ai.workflow.domain.service;

import com.ai.workflow.domain.model.OrchestratorWorkersResult;

/** Orchestrator plans subtasks; workers run in parallel; synthesizer combines outputs. */
public interface OrchestratorWorkersWorkflow {
  /** Splits the task, runs workers and joins the results. */
  OrchestratorWorkersResult process(String task);
}
