package com.ai.a2a.domain.model;

import java.util.Objects;
import lombok.Getter;

/** Declared skill on an Agent Card; id maps to a catalog Agent Type. */
@Getter
public final class A2aSkill {

  private final String id;
  private final String name;
  private final String description;

  private A2aSkill(String id, String name, String description) {
    this.id = Objects.requireNonNull(id, "id");
    this.name = Objects.requireNonNull(name, "name");
    this.description = description == null ? "" : description;
  }

  /** Creates a skill from catalog Agent metadata. */
  public static A2aSkill createSkill(String id, String name, String description) {
    return new A2aSkill(id, name, description);
  }
}
