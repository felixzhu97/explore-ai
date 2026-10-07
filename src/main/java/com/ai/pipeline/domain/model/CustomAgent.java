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
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Saved pipeline agent definition partitioned by owner_key. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class CustomAgent extends AbstractEnableableDescribedOwnerEntity<CustomAgentId> {

  private static final Pattern AGENT_TYPE_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");

  @NotBlank
  @Size(max = 64)
  @Column(nullable = false, length = 64, updatable = false)
  private String agentType;

  @NotBlank
  @Column(nullable = false, columnDefinition = "clob")
  private String systemPrompt;

  @Convert(converter = StringListJsonAttributeConverter.class)
  @Column(nullable = false, columnDefinition = "clob")
  private List<String> tools = new ArrayList<>();

  private CustomAgent(
      CustomAgentId id,
      String ownerKey,
      String agentType,
      String name,
      String description,
      String systemPrompt,
      List<String> tools) {
    super(id, ownerKey, name, description);
    this.agentType = requireAgentType(agentType);
    this.systemPrompt = DomainStrings.requireNonBlank(systemPrompt, "systemPrompt");
    this.tools = copyTools(tools);
  }

  /** Creates an enabled agent with a new id after validating its type key. */
  public static CustomAgent createAgent(
      String ownerKey,
      String agentType,
      String name,
      String description,
      String systemPrompt,
      List<String> tools) {
    return new CustomAgent(
        CustomAgentId.generateId(), ownerKey, agentType, name, description, systemPrompt, tools);
  }

  /** Replaces name, description, system prompt and tools, bumping the update timestamp. */
  public CustomAgent update(
      String name, String description, String systemPrompt, List<String> tools) {
    rename(name);
    updateDescription(description);
    this.systemPrompt = DomainStrings.requireNonBlank(systemPrompt, "systemPrompt");
    this.tools = copyTools(tools);
    return this;
  }

  /** Converts this saved entry into a single-runtime agent definition for the registry. */
  public AgentDefinition buildAgentDefinition() {
    return AgentDefinition.createDefinition(
        AgentType.createType(agentType),
        name,
        description,
        systemPrompt,
        getTools(),
        AgentDefinition.RUNTIME_SINGLE);
  }

  /** Tells whether this custom agent defines the agent type. */
  public boolean hasAgentType(AgentType type) {
    return type != null && agentType.equals(type.getValue());
  }

  /** Returns the tool keys as a read-only list. */
  public List<String> getTools() {
    return Collections.unmodifiableList(tools);
  }

  private static String requireAgentType(String agentType) {
    if (agentType == null || agentType.isBlank()) {
      throw new IllegalArgumentException("agentType cannot be blank");
    }
    String normalized = agentType.trim().toLowerCase(Locale.ROOT);
    if (!AGENT_TYPE_PATTERN.matcher(normalized).matches()) {
      throw new IllegalArgumentException("agentType must be lowercase alphanumeric with _ or -");
    }
    if ("supervisor".equals(normalized)) {
      throw new IllegalArgumentException("agentType cannot be supervisor");
    }
    return normalized;
  }

  private static List<String> copyTools(List<String> tools) {
    if (tools == null || tools.isEmpty()) {
      return new ArrayList<>();
    }
    List<String> copy = new ArrayList<>();
    for (String key : tools) {
      if (key != null && !key.isBlank()) {
        copy.add(key.trim());
      }
    }
    return copy;
  }
}
