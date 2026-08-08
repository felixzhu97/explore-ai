package com.ai.common.domain.vo;

import java.util.Objects;

/**
 * Data partition key: guest browser {@code c:{clientId}} or signed-in account {@code u:{accountUserId}}.
 */
public record OwnerKey(String value) {

    public static final String CLIENT_PREFIX = "c:";
    public static final String ACCOUNT_PREFIX = "u:";
    /** Pre-isolation rows that must never match a live visitor. */
    public static final OwnerKey LEGACY_ORPHAN = new OwnerKey("c:legacy-orphan");

    public OwnerKey {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("owner key is required");
        }
        if (!trimmed.startsWith(CLIENT_PREFIX) && !trimmed.startsWith(ACCOUNT_PREFIX)) {
            throw new IllegalArgumentException("owner key must start with c: or u:");
        }
        if (trimmed.length() <= 2) {
            throw new IllegalArgumentException("owner key id is required");
        }
        value = trimmed;
    }

    public static OwnerKey forClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required");
        }
        return new OwnerKey(CLIENT_PREFIX + clientId.trim());
    }

    public static OwnerKey forAccount(String accountUserId) {
        if (accountUserId == null || accountUserId.isBlank()) {
            throw new IllegalArgumentException("accountUserId is required");
        }
        return new OwnerKey(ACCOUNT_PREFIX + accountUserId.trim());
    }

    public static OwnerKey parse(String raw) {
        return new OwnerKey(raw);
    }

    public boolean isAccount() {
        return value.startsWith(ACCOUNT_PREFIX);
    }

    public boolean isClient() {
        return value.startsWith(CLIENT_PREFIX);
    }
}
