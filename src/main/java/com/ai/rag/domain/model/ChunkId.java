package com.ai.rag.domain.model;

import com.ai.common.domain.model.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for DocumentChunk. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class ChunkId extends AbstractUuidId {

  public ChunkId(String value) {
    super(value);
  }

  /** Wraps an existing id. */
  public static ChunkId of(String value) {
    return new ChunkId(value);
  }

  /** Creates a new random id. */
  public static ChunkId generate() {
    return new ChunkId(generateUuidString());
  }
}
