package com.ai.rag.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** DocumentId value object backed by UUID. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class DocumentId extends AbstractUuidId {

  /** Documentation. */
  public DocumentId(String value) {
    super(value);
  }

  /** Documentation. */
  public static DocumentId of(UUID uuid) {
    if (uuid == null) {
      throw new IllegalArgumentException("UUID cannot be null");
    }
    return new DocumentId(uuid.toString());
  }

  /** Documentation. */
  public static DocumentId of(String uuidString) {
    return new DocumentId(uuidString);
  }

  /** Documentation. */
  public static DocumentId generate() {
    return new DocumentId(newUuidString());
  }

  /** Documentation. */
  public UUID uuidValue() {
    return asUuid();
  }
}
