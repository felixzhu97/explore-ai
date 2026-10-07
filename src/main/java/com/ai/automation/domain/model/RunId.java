package com.ai.automation.domain.model;

import com.ai.common.domain.model.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.automation.domain.model.AutomationRun}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class RunId extends AbstractUuidId {

  public RunId(String value) {
    super(value);
  }

  /** Wraps an existing run id. */
  public static RunId of(String value) {
    return new RunId(value);
  }

  /** Creates a new random run id. */
  public static RunId generate() {
    return new RunId(generateUuidString());
  }
}
