package com.ai.automation.domain.repository;

/** Runs a saved workflow and returns a plain-text result for email delivery. */
public interface WorkflowRunner {
  String runSavedWorkflow(
      String ownerKey, String workflowTemplateId, String brief, String language);
}
