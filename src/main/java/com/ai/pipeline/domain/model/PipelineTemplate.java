package com.ai.pipeline.domain.model;

import com.ai.common.domain.model.AbstractEnableableDescribedOwnerEntity;
import com.ai.common.domain.model.DomainStrings;
import com.ai.common.domain.model.StringListJsonAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Saved multi-agent pipeline template partitioned by owner key. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class PipelineTemplate extends AbstractEnableableDescribedOwnerEntity<PipelineTemplateId> {

  /** Brief used when the user has not written one; the pipelines canvas uses the same text. */
  public static final String GENERIC_BRIEF =
      "Follow the configured agent pipeline for the user task.";

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

  private PipelineTemplate(
      PipelineTemplateId id,
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId) {
    super(id, ownerKey, name, description);
    this.agentTypes = copyAgentTypes(agentTypes);
    this.shortTopic = normalizeShortTopic(shortTopic);
    this.briefPrompt = DomainStrings.requireNonBlank(briefPrompt, "briefPrompt");
    this.sourceTemplateId = normalizeSourceTemplateId(sourceTemplateId);
  }

  /** Creates an enabled template with a new id, optionally linked to its catalog source. */
  public static PipelineTemplate create(
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String shortTopic,
      String briefPrompt,
      String sourceTemplateId) {
    return new PipelineTemplate(
        PipelineTemplateId.generate(),
        ownerKey,
        name,
        description,
        agentTypes,
        shortTopic,
        briefPrompt,
        sourceTemplateId);
  }

  /** Replaces the editable fields, normalizing agent types and bumping the update timestamp. */
  public PipelineTemplate update(
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
    return this;
  }

  /** Tells whether the template can be run, by hand or by a schedule. */
  public boolean isRunnable() {
    return isEnabled() && !agentTypes.isEmpty();
  }

  /** Builds a pipeline that runs the template's agents one after another. */
  public AgentPipeline toLinearPipeline() {
    List<AgentPipeline.PipelineNode> nodes = new ArrayList<>();
    List<AgentPipeline.PipelineEdge> edges = new ArrayList<>();
    for (int i = 0; i < agentTypes.size(); i++) {
      String id = "n" + i;
      nodes.add(AgentPipeline.PipelineNode.of(id, AgentType.of(agentTypes.get(i))));
      if (i > 0) {
        edges.add(new AgentPipeline.PipelineEdge("n" + (i - 1), id));
      }
    }
    return AgentPipeline.create(nodes, edges);
  }

  /**
   * Builds the first message of a run as {@code topic + "\n\n" + briefPrompt}, like the pipelines
   * canvas. A blank or generic schedule brief falls back to the template's short topic.
   */
  public String composeInvokeMessage(String scheduleBrief) {
    String instructions = briefPrompt == null ? "" : briefPrompt.trim();
    String topic;
    if (isGenericBrief(scheduleBrief)) {
      topic = shortTopic == null ? "" : shortTopic.trim();
    } else {
      topic = scheduleBrief.trim();
    }
    if (instructions.isBlank()) {
      return topic.isBlank() ? GENERIC_BRIEF : topic;
    }
    if (!topic.isBlank() && topic.contains(instructions)) {
      return topic;
    }
    if (topic.isBlank()) {
      return instructions;
    }
    return topic + "\n\n" + instructions;
  }

  /** Returns the agent types as a read-only list. */
  public List<String> getAgentTypes() {
    return Collections.unmodifiableList(agentTypes);
  }

  private static boolean isGenericBrief(String brief) {
    return brief == null
        || brief.isBlank()
        || brief.trim().toLowerCase(Locale.ROOT).equals(GENERIC_BRIEF.toLowerCase(Locale.ROOT));
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
      AgentType agentType = AgentType.of(type);
      if (agentType.isSupervisor()) {
        throw new IllegalArgumentException("Pipeline templates can only contain worker agents");
      }
      normalized.add(agentType.value());
    }
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Agent types cannot be empty");
    }
    return normalized;
  }
}
