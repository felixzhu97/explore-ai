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
  private String topic;

  @NotBlank
  @Column(nullable = false, columnDefinition = "clob")
  private String brief;

  @Size(max = 64)
  @Column(length = 64)
  private String builtinTemplateId;

  private PipelineTemplate(
      PipelineTemplateId id,
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String topic,
      String brief,
      String builtinTemplateId) {
    super(id, ownerKey, name, description);
    this.agentTypes = copyAgentTypes(agentTypes);
    this.topic = normalizeTopic(topic);
    this.brief = DomainStrings.requireNonBlank(brief, "brief");
    this.builtinTemplateId = normalizeBuiltinTemplateId(builtinTemplateId);
  }

  /** Creates an enabled template with a new id, optionally linked to its catalog source. */
  public static PipelineTemplate createTemplate(
      String ownerKey,
      String name,
      String description,
      List<String> agentTypes,
      String topic,
      String brief,
      String builtinTemplateId) {
    return new PipelineTemplate(
        PipelineTemplateId.generateId(),
        ownerKey,
        name,
        description,
        agentTypes,
        topic,
        brief,
        builtinTemplateId);
  }

  /** Replaces the editable fields, normalizing agent types and bumping the update timestamp. */
  public PipelineTemplate update(
      String name, String description, List<String> agentTypes, String topic, String brief) {
    rename(name);
    updateDescription(description);
    this.agentTypes = copyAgentTypes(agentTypes);
    this.topic = normalizeTopic(topic);
    this.brief = DomainStrings.requireNonBlank(brief, "brief");
    return this;
  }

  /** Tells whether the template can be run, by hand or by a schedule. */
  public boolean isRunnable() {
    return isEnabled() && !agentTypes.isEmpty();
  }

  /** Builds a pipeline that runs the template's agents one after another. */
  public AgentPipeline buildLinearPipeline() {
    List<AgentPipeline.PipelineNode> nodes = new ArrayList<>();
    List<AgentPipeline.PipelineEdge> edges = new ArrayList<>();
    for (int i = 0; i < agentTypes.size(); i++) {
      String id = "n" + i;
      nodes.add(AgentPipeline.PipelineNode.createNode(id, AgentType.createType(agentTypes.get(i))));
      if (i > 0) {
        edges.add(new AgentPipeline.PipelineEdge("n" + (i - 1), id));
      }
    }
    return AgentPipeline.createPipeline(nodes, edges);
  }

  /**
   * Builds the first message of a run as {@code topic + "\n\n" + brief}, like the pipelines canvas.
   * A blank or generic schedule brief falls back to the template's short topic.
   */
  public String composeFirstMessage(String scheduleBrief) {
    String instructions = brief == null ? "" : brief.trim();
    String topic;
    if (isGenericBrief(scheduleBrief)) {
      topic = this.topic == null ? "" : this.topic.trim();
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

  private static String normalizeTopic(String topic) {
    return DomainStrings.normalizeDescription(topic, 200);
  }

  private static String normalizeBuiltinTemplateId(String builtinTemplateId) {
    if (builtinTemplateId == null || builtinTemplateId.isBlank()) {
      return null;
    }
    return builtinTemplateId.trim();
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
      AgentType agentType = AgentType.createType(type);
      if (agentType.isSupervisor()) {
        throw new IllegalArgumentException("Pipeline templates can only contain worker agents");
      }
      normalized.add(agentType.getValue());
    }
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Agent types cannot be empty");
    }
    return normalized;
  }
}
