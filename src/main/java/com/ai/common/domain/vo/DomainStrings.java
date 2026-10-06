package com.ai.common.domain.vo;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Shared string normalization and validation for domain models. */
public final class DomainStrings {

  public static final int DEFAULT_NAME_MAX = 120;
  public static final int DEFAULT_DESCRIPTION_MAX = 500;
  private static final int MAX_COPY_NUMBER = 99;

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

  /** Returns the name as entities store it, so uniqueness checks compare like with like. */
  public static String normalizeName(String name) {
    return name == null ? null : name.trim();
  }

  /**
   * Returns copy names to try in order: the base name, then {@code base (2)} to {@code base (99)},
   * each cut to fit {@code maxLength}.
   */
  public static Stream<String> copyNameCandidates(String baseName, int maxLength) {
    String base = truncate(requireNonBlank(baseName, "name"), maxLength).trim();
    return Stream.concat(
        Stream.of(base),
        IntStream.rangeClosed(2, MAX_COPY_NUMBER)
            .mapToObj(number -> copyName(base, String.valueOf(number), maxLength)));
  }

  /** Returns {@code base (suffix)}, cutting the base so the result fits {@code maxLength}. */
  public static String copyName(String baseName, String suffix, int maxLength) {
    String tail = " (" + suffix + ")";
    String base = requireNonBlank(baseName, "name");
    return truncate(base, Math.max(1, maxLength - tail.length())).trim() + tail;
  }

  /** Returns the trimmed description, or empty when blank. */
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
