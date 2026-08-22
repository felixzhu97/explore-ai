package com.ai.skill.domain.model;

import com.ai.common.domain.model.AbstractEnableableDescribedOwnerEntity;
import com.ai.common.domain.vo.DomainStrings;
import com.ai.common.domain.vo.StringListJsonAttributeConverter;
import com.ai.skill.domain.vo.SkillId;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** User-defined skill aggregate partitioned by owner_key. */
@Entity
@Table(name = "skills")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Skill extends AbstractEnableableDescribedOwnerEntity<SkillId> {

  @Column(name = "instructions", nullable = false, columnDefinition = "clob")
  private String instructions;

  @Convert(converter = StringListJsonAttributeConverter.class)
  @Column(name = "allowed_tools", columnDefinition = "clob")
  private List<String> allowedTools = new ArrayList<>();

  private Skill(
      SkillId id,
      String ownerKey,
      String name,
      String description,
      String instructions,
      List<String> allowedTools,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    super(id, ownerKey, name, description, enabled, createdAt, updatedAt);
    this.instructions = DomainStrings.requireNonBlank(instructions, "instructions");
    this.allowedTools = copyAllowedTools(allowedTools);
  }

  /** Documentation. */
  public static Skill create(
      String ownerKey,
      String name,
      String description,
      String instructions,
      List<String> allowedTools) {
    Instant now = Instant.now();
    return new Skill(
        SkillId.generate(),
        ownerKey,
        name,
        description,
        instructions,
        allowedTools,
        true,
        now,
        now);
  }

  /** Documentation. */
  public static Skill restore(
      SkillId id,
      String ownerKey,
      String name,
      String description,
      String instructions,
      List<String> allowedTools,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    return new Skill(
        id, ownerKey, name, description, instructions, allowedTools, enabled, createdAt, updatedAt);
  }

  /** Documentation. */
  public Skill update(
      String name, String description, String instructions, List<String> allowedTools) {
    rename(name);
    updateDescription(description);
    this.instructions = DomainStrings.requireNonBlank(instructions, "instructions");
    this.allowedTools = copyAllowedTools(allowedTools);
    touchUpdatedAt();
    return this;
  }

  /** Documentation. */
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
