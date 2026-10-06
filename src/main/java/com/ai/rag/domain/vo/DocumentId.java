package com.ai.rag.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** DocumentId value object backed by UUID. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class DocumentId extends AbstractUuidId {

  public DocumentId(String value) {
    super(value);
  }

  /** Creates an id from the UUID, rejecting null. */
  public static DocumentId of(UUID uuid) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }
    return new DocumentId(uuid.toString());
  }

  /** Wraps an existing UUID string. */
  public static DocumentId of(String uuidString) {
    return new DocumentId(uuidString);
  }

  /** Creates a new random id. */
  public static DocumentId generate() {
    return new DocumentId(generateUuidString());
  }

  /** Returns the id as a UUID. */
  public UUID uuidValue() {
    return asUuid();
  }
}
