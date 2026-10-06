package com.ai.automation.domain.vo;

import com.ai.common.domain.vo.AbstractUuidId;
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

  public static ScheduleId of(String value) {
    return new ScheduleId(value);
  }

  public static ScheduleId generate() {
    return new ScheduleId(generateUuidString());
  }
}
