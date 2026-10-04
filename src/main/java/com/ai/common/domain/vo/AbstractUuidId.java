package com.ai.common.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Shared UUID id field for {@code @Embeddable} feature-module ID types. */
@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractUuidId implements EntityId {

  @EqualsAndHashCode.Include
  @Column(name = "id", nullable = false)
  protected UUID value;

  protected AbstractUuidId(String value) {
    this.value = UUID.fromString(requireUuid(value));
  }

  protected static String requireUuid(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Id value cannot be null or blank");
    }
    String trimmed = value.trim();
    UUID.fromString(trimmed);
    return trimmed;
  }

  protected static String newUuidString() {
    return UUID.randomUUID().toString();
  }

  public UUID asUuid() {
    return value;
  }

  @Override
  public String value() {
    return value.toString();
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
