package com.ai.rag.domain.model;

import com.ai.common.domain.model.AbstractEmbeddable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/** Strongly-typed ID for DocumentChunk. */
@Getter
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(staticName = "createId")
public final class ChunkId extends AbstractEmbeddable {

  @NonNull private UUID value;

  /** Parses an id from its UUID text. */
  public static ChunkId parseId(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Id cannot be blank");
    }
    return createId(UUID.fromString(text.strip()));
  }

  /** Creates a new random id. */
  public static ChunkId generateId() {
    return createId(UUID.randomUUID());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
