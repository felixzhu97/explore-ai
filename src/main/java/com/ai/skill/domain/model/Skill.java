package com.ai.skill.domain.model;

import com.ai.common.domain.model.AbstractEnableableDescribedOwnerEntity;
import com.ai.common.domain.model.DomainStrings;
import com.ai.common.domain.model.StringListJsonAttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** User-defined skill aggregate partitioned by owner_key. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Skill extends AbstractEnableableDescribedOwnerEntity<SkillId> {

  @NotBlank
  @Column(nullable = false, columnDefinition = "clob")
  private String instructions;

  @Convert(converter = StringListJsonAttributeConverter.class)
  @Column(columnDefinition = "clob")
  private List<String> allowedTools = new ArrayList<>();

  private Skill(
      SkillId id,
      String ownerKey,
      String name,
      String description,
      String instructions,
      List<String> allowedTools) {
    super(id, ownerKey, name, description);
    this.instructions = DomainStrings.requireNonBlank(instructions, "instructions");
    this.allowedTools = copyAllowedTools(allowedTools);
  }

  /** Creates a new enabled skill for the owner with a generated id. */
  public static Skill createSkill(
      String ownerKey,
      String name,
      String description,
      String instructions,
      List<String> allowedTools) {
    return new Skill(SkillId.generateId(), ownerKey, name, description, instructions, allowedTools);
  }

  /** Replaces name, description, instructions, and allowed tools, then bumps the update time. */
  public Skill update(
      String name, String description, String instructions, List<String> allowedTools) {
    rename(name);
    updateDescription(description);
    this.instructions = DomainStrings.requireNonBlank(instructions, "instructions");
    this.allowedTools = copyAllowedTools(allowedTools);
    return this;
  }

  /** Renders the skill as a section of the chat system prompt. */
  public String buildPromptSection() {
    StringBuilder section = new StringBuilder("### ").append(getName()).append('\n');
    if (getDescription() != null && !getDescription().isBlank()) {
      section.append(getDescription()).append('\n');
    }
    return section.append(instructions).toString();
  }

  /** Returns the allowed tools as a read-only list. */
  public List<String> getAllowedTools() {
    return Collections.unmodifiableList(allowedTools == null ? List.of() : allowedTools);
  }

  private static List<String> copyAllowedTools(List<String> allowedTools) {
    if (allowedTools == null || allowedTools.isEmpty()) {
      return new ArrayList<>();
    }
    return new ArrayList<>(allowedTools);
  }
}
