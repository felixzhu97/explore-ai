package com.ai.pipeline.domain.model;

import com.ai.common.domain.model.AbstractEnableableDescribedOwnerEntity;
import com.ai.common.domain.vo.DomainStrings;
import com.ai.common.infra.persistence.converter.StringListJsonAttributeConverter;
import com.ai.pipeline.domain.vo.WorkflowTemplateId;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Saved multi-agent workflow template partitioned by owner_key. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class SavedWorkflowTemplate
    extends AbstractEnableableDescribedOwnerEntity<WorkflowTemplateId> {

  @Convert(converter = StringListJsonAttributeConverter.class)
  @Column(nullable = false, columnDefinition = "clob")
  private List<String> agentTypes = new ArrayList<>();

  @Size(max = 200)
  @Column(length = 200)
  private String shortTopic;

  @NotBlank
  @Column(nullable = false, columnDefinition = "clob")
  private String briefPrompt;

  @Size(max = 64)
  @Column(length = 64)
  private String sourceTemplateId;

  private SavedWorkflowTemplate(
      WorkflowTemplateId id,
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, description, enabled, createdAt, updatedAt);
    this.agentTypes = copyAgentTypes(agentTypes);
    this.shortTopic = normalizeShortTopic(shortTopic);
    this.briefPrompt = DomainStrings.requireNonBlank(briefPrompt, "briefPrompt");
    this.sourceTemplateId = normalizeSourceTemplateId(sourceTemplateId);
  }

  /** Documentation. */
  public static SavedWorkflowTemplate create(
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId) {
    Instant now = Instant.now();
    return new SavedWorkflowTemplate(
        WorkflowTemplateId.generate(),
        ownerKey,
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
  public static SavedWorkflowTemplate reconstitute(
      WorkflowTemplateId id,
      String ownerKey,
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
        ownerKey,
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
    rename(name);
    updateDescription(description);
    this.agentTypes = copyAgentTypes(agentTypes);
    this.shortTopic = normalizeShortTopic(shortTopic);
    this.briefPrompt = DomainStrings.requireNonBlank(briefPrompt, "briefPrompt");
    touchUpdatedAt();
    return this;
  }

  /** Documentation. */
  public List<String> getAgentTypes() {
    return Collections.unmodifiableList(agentTypes == null ? List.of() : agentTypes);
  }

  private static String normalizeShortTopic(String shortTopic) {
    return DomainStrings.normalizeDescription(shortTopic, 200);
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
    return normalized;
  }
}
