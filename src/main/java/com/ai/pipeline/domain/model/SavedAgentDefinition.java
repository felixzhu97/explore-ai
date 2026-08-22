package com.ai.pipeline.domain.model;

import com.ai.common.domain.model.AbstractEnableableDescribedOwnerEntity;
import com.ai.common.domain.vo.DomainStrings;
import com.ai.common.domain.vo.StringListJsonAttributeConverter;
import com.ai.pipeline.domain.vo.AgentType;
import com.ai.pipeline.domain.vo.SavedAgentId;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
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
@Table(name = "pipeline_agents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class SavedAgentDefinition extends AbstractEnableableDescribedOwnerEntity<SavedAgentId> {

  private static final Pattern TYPE_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");

  @Column(name = "type_key", nullable = false, length = 64, updatable = false)
  private String typeKey;

  @Column(name = "system_prompt", nullable = false, columnDefinition = "clob")
  private String systemPrompt;

  @Convert(converter = StringListJsonAttributeConverter.class)
  @Column(name = "tool_keys", nullable = false, columnDefinition = "clob")
  private List<String> toolKeys = new ArrayList<>();

  private SavedAgentDefinition(
      SavedAgentId id,
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, description, enabled, createdAt, updatedAt);
    this.typeKey = requireTypeKey(typeKey);
    this.systemPrompt = DomainStrings.requireNonBlank(systemPrompt, "systemPrompt");
    this.toolKeys = copyToolKeys(toolKeys);
  }

  /** Documentation. */
  public static SavedAgentDefinition create(
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    Instant now = Instant.now();
    return new SavedAgentDefinition(
        SavedAgentId.generate(),
        ownerKey,
        typeKey,
        name,
        description,
        systemPrompt,
        toolKeys,
        true,
        now,
        now);
  }

  /** Documentation. */
  public static SavedAgentDefinition restore(
      SavedAgentId id,
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    return new SavedAgentDefinition(
        id,
        ownerKey,
        typeKey,
        name,
        description,
        systemPrompt,
        toolKeys,
        enabled,
        createdAt,
        updatedAt);
  }

  /** Documentation. */
  public SavedAgentDefinition update(
      String name, String description, String systemPrompt, List<String> toolKeys) {
    rename(name);
    updateDescription(description);
    this.systemPrompt = DomainStrings.requireNonBlank(systemPrompt, "systemPrompt");
    this.toolKeys = copyToolKeys(toolKeys);
    touchUpdatedAt();
    return this;
  }

  /** Documentation. */
  public AgentDefinition toAgentDefinition() {
    return AgentDefinition.create(
        AgentType.of(typeKey),
        name,
        description,
        systemPrompt,
        getToolKeys(),
        AgentDefinition.RUNTIME_SINGLE);
  }

  /** Documentation. */
  public List<String> getToolKeys() {
    return Collections.unmodifiableList(toolKeys == null ? List.of() : toolKeys);
  }

  private static String requireTypeKey(String typeKey) {
    if (typeKey == null || typeKey.isBlank()) {
      throw new IllegalArgumentException("typeKey cannot be blank");
    }
    String normalized = typeKey.trim().toLowerCase(Locale.ROOT);
    if (!TYPE_KEY_PATTERN.matcher(normalized).matches()) {
      throw new IllegalArgumentException("typeKey must be lowercase alphanumeric with _ or -");
    }
    if ("supervisor".equals(normalized)) {
      throw new IllegalArgumentException("typeKey cannot be supervisor");
    }
    return normalized;
  }

  private static List<String> copyToolKeys(List<String> toolKeys) {
    if (toolKeys == null || toolKeys.isEmpty()) {
      return new ArrayList<>();
    }
    List<String> copy = new ArrayList<>();
    for (String key : toolKeys) {
      if (key != null && !key.isBlank()) {
        copy.add(key.trim());
      }
    }
    return copy;
  }
}
