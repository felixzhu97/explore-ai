package com.ai.automation.domain.model;

import com.ai.common.domain.model.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.automation.domain.model.AutomationSchedule}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class ScheduleId extends AbstractUuidId {

  public ScheduleId(String value) {
    super(value);
  }

  /** Wraps an existing schedule id. */
  public static ScheduleId of(String value) {
    return new ScheduleId(value);
  }

  /** Creates a new random schedule id. */
  public static ScheduleId generate() {
    return new ScheduleId(generateUuidString());
  }
}
