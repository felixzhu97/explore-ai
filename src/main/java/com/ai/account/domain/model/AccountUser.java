package com.ai.account.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Linked OAuth identity for a browser Client Identity partition.
 */
public final class AccountUser {

    private final String id;
    private final String provider;
    private final String subject;
    private String email;
    private String linkedClientId;
    private final Instant createdAt;
    private Instant updatedAt;

    private AccountUser(
            String id,
            String provider,
            String subject,
            String email,
            String linkedClientId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.provider = requireProvider(provider);
        this.subject = requireSubject(subject);
        this.email = normalizeEmail(email);
        this.linkedClientId = normalizeClientId(linkedClientId);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static AccountUser create(String provider, String subject, String email, String linkedClientId) {
        Instant now = Instant.now();
        return new AccountUser(
                UUID.randomUUID().toString(), provider, subject, email, linkedClientId, now, now);
    }

    public static AccountUser restore(
            String id,
            String provider,
            String subject,
            String email,
            String linkedClientId,
            Instant createdAt,
            Instant updatedAt) {
        return new AccountUser(id, provider, subject, email, linkedClientId, createdAt, updatedAt);
    }

    public void linkSession(String email, String linkedClientId) {
        this.email = normalizeEmail(email);
        this.linkedClientId = requireClientId(linkedClientId);
        this.updatedAt = Instant.now();
    }

    /** Clears the browser partition link so logout returns to guest mode. */
    public void unlinkBrowser() {
        this.linkedClientId = null;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public String getLinkedClientId() {
        return linkedClientId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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
