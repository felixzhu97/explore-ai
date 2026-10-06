package com.ai.rag.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
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

  public static ChunkId of(String value) {
    return new ChunkId(value);
  }

  public static ChunkId generate() {
    return new ChunkId(generateUuidString());
  }
}
