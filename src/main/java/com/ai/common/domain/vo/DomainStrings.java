package com.ai.common.domain.vo;

/** Shared string normalization and validation for domain models. */
public final class DomainStrings {

  public static final int DEFAULT_NAME_MAX = 120;
  public static final int DEFAULT_DESCRIPTION_MAX = 500;

  private DomainStrings() {}

  /** Returns the trimmed name, rejecting blank or too long names. */
  public static String requireName(String name) {
    return requireName(name, DEFAULT_NAME_MAX);
  }

  /** Returns the trimmed name, rejecting blank values or names longer than {@code maxLength}. */
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

  /** Returns the trimmed description, or null when blank. */
  public static String normalizeDescription(String description) {
    return normalizeDescription(description, DEFAULT_DESCRIPTION_MAX);
  }

  /** Returns the trimmed description cut to {@code maxLength}, or empty when blank. */
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

  /** Returns the trimmed value, throwing with the field name when it is null or blank. */
  public static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }

  /** Cuts the value to at most {@code maxLength} characters, passing null through. */
  public static String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
