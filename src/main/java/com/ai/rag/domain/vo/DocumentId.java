package com.ai.rag.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * DocumentId Value Object
 * 
 * Represents a unique identifier for documents using UUID.
 * Value objects are immutable and compared by value equality.
 */
public final class DocumentId {
    private final UUID value;

    private DocumentId(UUID value) {
        this.value = value;
    }

    /**
     * Creates a DocumentId from a UUID.
     */
    public static DocumentId of(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        return new DocumentId(uuid);
    }

    /**
     * Creates a DocumentId from a string representation of a UUID.
     */
    public static DocumentId of(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new IllegalArgumentException("UUID string cannot be null or blank");
        }
        return new DocumentId(UUID.fromString(uuidString));
    }

    /**
     * Generates a new random DocumentId.
     */
    public static DocumentId generate() {
        return new DocumentId(UUID.randomUUID());
    }

    /**
     * Returns the underlying UUID value.
     */
    public UUID value() {
        return value;
    }

    /**
     * Returns the string representation of the UUID.
     */
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentId that = (DocumentId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
