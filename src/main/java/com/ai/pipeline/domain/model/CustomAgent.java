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

  private static final Pattern TYPE_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");

  @NotBlank
  @Size(max = 64)
  @Column(nullable = false, length = 64, updatable = false)
  private String typeKey;

  @NotBlank
  @Column(nullable = false, columnDefinition = "clob")
  private String systemPrompt;

  @Convert(converter = StringListJsonAttributeConverter.class)
  @Column(nullable = false, columnDefinition = "clob")
  private List<String> toolKeys = new ArrayList<>();

  private CustomAgent(
      CustomAgentId id,
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    super(id, ownerKey, name, description);
    this.typeKey = requireTypeKey(typeKey);
    this.systemPrompt = DomainStrings.requireNonBlank(systemPrompt, "systemPrompt");
    this.toolKeys = copyToolKeys(toolKeys);
  }

  /** Creates an enabled agent with a new id after validating its type key. */
  public static CustomAgent create(
      String ownerKey,
      String typeKey,
      String name,
      String description,
      String systemPrompt,
      List<String> toolKeys) {
    return new CustomAgent(
        CustomAgentId.generate(), ownerKey, typeKey, name, description, systemPrompt, toolKeys);
  }

  /** Replaces name, description, system prompt and tools, bumping the update timestamp. */
  public CustomAgent update(
      String name, String description, String systemPrompt, List<String> toolKeys) {
    rename(name);
    updateDescription(description);
    this.systemPrompt = DomainStrings.requireNonBlank(systemPrompt, "systemPrompt");
    this.toolKeys = copyToolKeys(toolKeys);
    return this;
  }

  /** Converts this saved entry into a single-runtime agent definition for the registry. */
  public AgentDefinition toAgentDefinition() {
    return AgentDefinition.create(
        AgentType.of(typeKey),
        name,
        description,
        systemPrompt,
        getToolKeys(),
        AgentDefinition.RUNTIME_SINGLE);
  }

  /** Tells whether this custom agent defines the agent type. */
  public boolean hasType(AgentType type) {
    return type != null && typeKey.equals(type.value());
  }

  /** Returns the tool keys as a read-only list. */
  public List<String> getToolKeys() {
    return Collections.unmodifiableList(toolKeys);
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
