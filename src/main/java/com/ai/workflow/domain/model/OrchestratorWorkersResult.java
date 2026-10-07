package com.ai.workflow.domain.model;

import java.util.List;
import lombok.Value;

/** Orchestrator analysis, planned tasks, parallel worker outputs, and synthesis. */
@Value
public class OrchestratorWorkersResult {
  String analysis;
  List<WorkerTask> tasks;
  List<String> workerResponses;
  String synthesis;

  public OrchestratorWorkersResult(
      String analysis, List<WorkerTask> tasks, List<String> workerResponses, String synthesis) {
    tasks = tasks == null ? List.of() : List.copyOf(tasks);
    workerResponses = workerResponses == null ? List.of() : List.copyOf(workerResponses);
    this.analysis = analysis;
    this.tasks = tasks;
    this.workerResponses = workerResponses;
    this.synthesis = synthesis;
  }
}
