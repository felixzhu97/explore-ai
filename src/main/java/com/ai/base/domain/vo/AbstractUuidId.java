package com.ai.base.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Embeddable String UUID identifier; feature-module IDs extend this type. */
@Embeddable
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public abstract class AbstractUuidId implements EntityId {

  @EqualsAndHashCode.Include
  @Convert(converter = UuidStringAttributeConverter.class)
  @Column(name = "id", nullable = false, length = 36)
  protected String value;

  /** Documentation. */
  protected AbstractUuidId(String value) {
    this.value = requireUuid(value);
  }

  /** Documentation. */
  protected static String requireUuid(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Id value cannot be null or blank");
    }
    String trimmed = value.trim();
    UUID.fromString(trimmed);
    return trimmed;
  }

  /** Documentation. */
  protected static String newUuidString() {
    return UUID.randomUUID().toString();
  }

  /** Documentation. */
  public UUID asUuid() {
    return UUID.fromString(value);
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
