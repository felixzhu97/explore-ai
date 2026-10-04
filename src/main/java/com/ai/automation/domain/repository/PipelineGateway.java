package com.ai.automation.domain.repository;

/** Runs a saved pipeline template and returns a plain-text result for email delivery. */
public interface PipelineGateway {
  String runSavedTemplate(
      String ownerKey, String pipelineTemplateId, String brief, String language);
}
