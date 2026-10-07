package com.ai.workflow.domain.model;

import lombok.Value;

/** Subtask planned by the orchestrator for a worker LLM. */
@Value
public class WorkerTask {
  String type;
  String description;
}
