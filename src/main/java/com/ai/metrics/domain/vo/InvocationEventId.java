package com.ai.metrics.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for AiInvocationEvent. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class InvocationEventId extends AbstractUuidId {

  public InvocationEventId(String value) {
    super(value);
  }

  /** Wraps an existing id. */
  public static InvocationEventId of(String value) {
    return new InvocationEventId(value);
  }

  /** Creates a new random id. */
  public static InvocationEventId generate() {
    return new InvocationEventId(generateUuidString());
  }
}
