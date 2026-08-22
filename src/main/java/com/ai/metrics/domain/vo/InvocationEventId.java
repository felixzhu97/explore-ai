package com.ai.metrics.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for AiInvocationEvent. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class InvocationEventId extends AbstractUuidId {

  /** Documentation. */
  public InvocationEventId(String value) {
    super(value);
  }

  /** Documentation. */
  public static InvocationEventId of(String value) {
    return new InvocationEventId(value);
  }

  /** Documentation. */
  public static InvocationEventId generate() {
    return new InvocationEventId(newUuidString());
  }
}
