package com.ai.account.domain.model;

import com.ai.account.domain.vo.AccountUserId;
import com.ai.base.domain.model.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Linked OAuth identity for a browser Client Identity partition. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class AccountUser extends AbstractEntity<AccountUserId> {

  @Column(nullable = false, length = 32)
  private String provider;

  @Column(nullable = false, length = 255)
  private String subject;

  @Column(length = 320)
  private String email;

  @Column(length = 64)
  private String linkedClientId;

  /** Documentation. */
  public static AccountUser create(
      String provider, String subject, String email, String linkedClientId) {
    AccountUser user = new AccountUser();
    user.id = AccountUserId.generate();
    user.provider = requireProvider(provider);
    user.subject = requireSubject(subject);
    user.email = normalizeEmail(email);
    user.linkedClientId = normalizeClientId(linkedClientId);
    Instant now = Instant.now();
    user.createdAt = now;
    user.updatedAt = now;
    return user;
  }

  /** Documentation. */
  public static AccountUser reconstitute(
      AccountUserId id,
      String provider,
      String subject,
      String email,
      String linkedClientId,
      Instant createdAt,
      Instant updatedAt) {
    AccountUser user = new AccountUser();
    user.id = id;
    user.provider = requireProvider(provider);
    user.subject = requireSubject(subject);
    user.email = normalizeEmail(email);
    user.linkedClientId = normalizeClientId(linkedClientId);
    user.createdAt = createdAt;
    user.updatedAt = updatedAt;
    return user;
  }

  /** Documentation. */
  public static AccountUser reconstitute(
      String id,
      String provider,
      String subject,
      String email,
      String linkedClientId,
      Instant createdAt,
      Instant updatedAt) {
    return reconstitute(
        AccountUserId.of(id), provider, subject, email, linkedClientId, createdAt, updatedAt);
  }

  /** Documentation. */
  public void linkSession(String email, String linkedClientId) {
    this.email = normalizeEmail(email);
    this.linkedClientId = requireClientId(linkedClientId);
    touchUpdatedAt();
  }

  /** Clears the browser partition link so logout returns to guest mode. */
  public void unlinkBrowser() {
    this.linkedClientId = null;
    touchUpdatedAt();
  }

  private static String requireProvider(String provider) {
    if (provider == null || provider.isBlank()) {
      throw new IllegalArgumentException("provider is required");
    }
    return provider.trim().toLowerCase();
  }

  private static String requireSubject(String subject) {
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("subject is required");
    }
    return subject.trim();
  }

  private static String requireClientId(String clientId) {
    String normalized = normalizeClientId(clientId);
    if (normalized == null) {
      throw new IllegalArgumentException("linkedClientId is required");
    }
    return normalized;
  }

  private static String normalizeClientId(String clientId) {
    if (clientId == null || clientId.isBlank()) {
      return null;
    }
    return clientId.trim();
  }

  private static String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return email.trim();
  }
}
