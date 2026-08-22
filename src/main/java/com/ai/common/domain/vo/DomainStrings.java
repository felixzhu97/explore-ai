package com.ai.common.domain.vo;

/** Shared string normalization and validation for domain models. */
public final class DomainStrings {

  public static final int DEFAULT_NAME_MAX = 120;
  public static final int DEFAULT_DESCRIPTION_MAX = 500;

  private DomainStrings() {}

  /** Documentation. */
  public static String requireName(String name) {
    return requireName(name, DEFAULT_NAME_MAX);
  }

  /** Documentation. */
  public static String requireName(String name, int maxLength) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name is required");
    }
    String trimmed = name.trim();
    if (trimmed.length() > maxLength) {
      throw new IllegalArgumentException("name exceeds " + maxLength + " characters");
    }
    return trimmed;
  }

  /** Documentation. */
  public static String normalizeDescription(String description) {
    return normalizeDescription(description, DEFAULT_DESCRIPTION_MAX);
  }

  /** Documentation. */
  public static String normalizeDescription(String description, int maxLength) {
    if (description == null || description.isBlank()) {
      return "";
    }
    String trimmed = description.trim();
    if (trimmed.length() <= maxLength) {
      return trimmed;
    }
    return trimmed.substring(0, maxLength);
  }

  /** Documentation. */
  public static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  /** Documentation. */
  public static String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
