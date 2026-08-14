package com.ai.pipeline.domain.model;

import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Documentation. */
public class SavedWorkflowTemplate {

  private final WorkflowTemplateId id;
  private final String clientId;
  private String name;
  private String description;
  private List<String> agentTypes;
  private String shortTopic;
  private String briefPrompt;
  private String sourceTemplateId;
  private boolean enabled;
  private final Instant createdAt;
  private Instant updatedAt;

  private SavedWorkflowTemplate(
      WorkflowTemplateId id,
      String clientId,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "WorkflowTemplateId cannot be null");
    this.clientId = requireClientId(clientId);
    this.name = requireName(name);
    this.description = normalizeDescription(description);
    this.agentTypes = copyAgentTypes(agentTypes);
    this.shortTopic = normalizeShortTopic(shortTopic);
    this.briefPrompt = requireBriefPrompt(briefPrompt);
    this.sourceTemplateId = normalizeSourceTemplateId(sourceTemplateId);
    this.enabled = enabled;
    this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
  }

  /** Documentation. */
  public static SavedWorkflowTemplate create(
      String clientId,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId) {
    Instant now = Instant.now();
    return new SavedWorkflowTemplate(
        WorkflowTemplateId.generate(),
        clientId,
        name,
        description,
        agentTypes,
        shortTopic,
        briefPrompt,
        sourceTemplateId,
        true,
        now,
        now);
  }

  /** Documentation. */
  public static SavedWorkflowTemplate restore(
      WorkflowTemplateId id,
      String clientId,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    return new SavedWorkflowTemplate(
        id,
        clientId,
        name,
        description,
        agentTypes,
        shortTopic,
        briefPrompt,
        sourceTemplateId,
        enabled,
        createdAt,
        updatedAt);
  }

  /** Documentation. */
  public SavedWorkflowTemplate update(
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt) {
    this.name = requireName(name);
    this.description = normalizeDescription(description);
    this.agentTypes = copyAgentTypes(agentTypes);
    this.shortTopic = normalizeShortTopic(shortTopic);
    this.briefPrompt = requireBriefPrompt(briefPrompt);
    this.updatedAt = Instant.now();
    return this;
  }

  /** Documentation. */
  public void enable() {
    this.enabled = true;
    this.updatedAt = Instant.now();
  }

  /** Documentation. */
  public void disable() {
    this.enabled = false;
    this.updatedAt = Instant.now();
  }

  public WorkflowTemplateId getId() {
    return id;
  }

  public String getClientId() {
    return clientId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getAgentTypes() {
    return Collections.unmodifiableList(agentTypes);
  }

  public String getShortTopic() {
    return shortTopic;
  }

  public String getBriefPrompt() {
    return briefPrompt;
  }

  public String getSourceTemplateId() {
    return sourceTemplateId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  private static String requireClientId(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      throw new IllegalArgumentException("ClientId cannot be null or blank");
    }
    return clientId.trim();
  }

  private static String requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Workflow template name cannot be null or blank");
    }
    String trimmed = name.trim();
    if (trimmed.length() > 120) {
      throw new IllegalArgumentException("Workflow template name cannot exceed 120 characters");
    }
    return trimmed;
  }

  private static String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return "";
    }
    String trimmed = description.trim();
    return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
  }

  private static String normalizeShortTopic(String shortTopic) {
    if (shortTopic == null || shortTopic.isBlank()) {
      return "";
    }
    String trimmed = shortTopic.trim();
    return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
  }

  private static String requireBriefPrompt(String briefPrompt) {
    if (briefPrompt == null || briefPrompt.isBlank()) {
      throw new IllegalArgumentException("Brief prompt cannot be null or blank");
    }
    return briefPrompt.trim();
  }

  private static String normalizeSourceTemplateId(String sourceTemplateId) {
    if (sourceTemplateId == null || sourceTemplateId.isBlank()) {
      return null;
    }
    return sourceTemplateId.trim();
  }

  private static List<String> copyAgentTypes(List<String> agentTypes) {
    if (agentTypes == null || agentTypes.isEmpty()) {
      throw new IllegalArgumentException("Agent types cannot be empty");
    }
    List<String> normalized = new ArrayList<>();
    for (String type : agentTypes) {
      if (type == null || type.isBlank()) {
        continue;
      }
      normalized.add(type.trim().toLowerCase(Locale.ROOT));
    }
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Agent types cannot be empty");
    }
    return Collections.unmodifiableList(normalized);
  }
}
