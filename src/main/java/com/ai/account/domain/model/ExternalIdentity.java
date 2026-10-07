package com.ai.account.domain.model;

import java.util.Locale;

/** Sign-in provider and subject pair, normalized the same way for saving and lookup. */
public record ExternalIdentity(String provider, String subject) {

  public static final String EXPLORE_IAM = "explore-iam";

  static final String UNKNOWN_PROVIDER = "unknown";
  static final int MAX_PROVIDER_LENGTH = 32;
  static final int MAX_SUBJECT_LENGTH = 255;

  public ExternalIdentity {
    provider = normalizeProvider(provider);
    subject = normalizeSubject(subject);
  }

  /** Normalizes a provider registration id and subject. */
  public static ExternalIdentity createIdentity(String provider, String subject) {
    return new ExternalIdentity(provider, subject);
  }

  /** Identity of an Explore IAM access token subject. */
  public static ExternalIdentity createIamIdentity(String subject) {
    return new ExternalIdentity(EXPLORE_IAM, subject);
  }

  private static String normalizeProvider(String provider) {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("provider is required");
    }
    String normalized = provider.trim().toLowerCase(Locale.ROOT);
    if (UNKNOWN_PROVIDER.equals(normalized)) {
      throw new IllegalArgumentException("provider is unknown");
    }
    if (normalized.length() > MAX_PROVIDER_LENGTH) {
      throw new IllegalArgumentException("provider is too long");
    }
    return normalized;
  }

  private static String normalizeSubject(String subject) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("subject is required");
    }
    String normalized = subject.trim();
    if (normalized.length() > MAX_SUBJECT_LENGTH) {
      throw new IllegalArgumentException("subject is too long");
    }
    return normalized;
  }
}
