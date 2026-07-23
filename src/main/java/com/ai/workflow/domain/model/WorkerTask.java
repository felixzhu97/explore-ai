package com.ai.workflow.domain.model;

/**
 * Subtask planned by the orchestrator for a worker LLM.
 */
public record WorkerTask(String type, String description) {
}
