package com.ai.automation.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.automation.domain.model.AutomationRun}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class RunId extends AbstractUuidId {

  /** Documentation. */
  public RunId(String value) {
    super(value);
  }

  /** Documentation. */
  public static RunId of(String value) {
    return new RunId(value);
  }

  /** Documentation. */
  public static RunId generate() {
    return new RunId(newUuidString());
  }
}
