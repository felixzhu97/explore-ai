package com.ai.automation.domain.vo;

import com.ai.base.domain.vo.AbstractUuidId;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Strongly-typed ID for {@link com.ai.automation.domain.model.AutomationSchedule}. */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class ScheduleId extends AbstractUuidId {

  /** Documentation. */
  public ScheduleId(String value) {
    super(value);
  }

  /** Documentation. */
  public static ScheduleId of(String value) {
    return new ScheduleId(value);
  }

  /** Documentation. */
  public static ScheduleId generate() {
    return new ScheduleId(newUuidString());
  }
}
