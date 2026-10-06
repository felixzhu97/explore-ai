package com.ai.automation.domain.vo;

import com.ai.common.domain.vo.DomainStrings;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** When a schedule fires: a cron expression or a single run, read in one time zone. */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public final class ScheduleTiming {

  private static final int MAX_CRON = 80;
  private static final int MAX_TIMEZONE = 64;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ScheduleKind scheduleKind;

  @Size(max = MAX_CRON)
  @Column(length = MAX_CRON)
  private String cronExpression;

  @NotBlank
  @Size(max = MAX_TIMEZONE)
  @Column(nullable = false, length = MAX_TIMEZONE)
  private String timezone;

  private ScheduleTiming(ScheduleKind scheduleKind, String cronExpression, String timezone) {
    this.scheduleKind = scheduleKind;
    this.cronExpression = cronExpression;
    this.timezone = requireTimezone(timezone);
  }

  /** Repeats on the cron expression, evaluated in the time zone. */
  public static ScheduleTiming cron(String cronExpression, String timezone) {
    String cron = DomainStrings.requireNonBlank(cronExpression, "cronExpression").trim();
    if (cron.length() > MAX_CRON) {
      throw new IllegalArgumentException("cronExpression is too long");
    }
    return new ScheduleTiming(ScheduleKind.CRON, cron, timezone);
  }

  /** Runs once at an instant chosen by the user in the time zone. */
  public static ScheduleTiming once(String timezone) {
    return new ScheduleTiming(ScheduleKind.ONCE, null, timezone);
  }

  /** Builds the timing for the kind; the cron expression is ignored for one-off runs. */
  public static ScheduleTiming of(ScheduleKind kind, String cronExpression, String timezone) {
    return Objects.requireNonNull(kind, "scheduleKind") == ScheduleKind.ONCE
        ? once(timezone)
        : cron(cronExpression, timezone);
  }

  /** Tells whether the schedule runs only once. */
  public boolean isOnce() {
    return scheduleKind == ScheduleKind.ONCE;
  }

  private static String requireTimezone(String timezone) {
    String zone = DomainStrings.requireNonBlank(timezone, "timezone").trim();
    if (zone.length() > MAX_TIMEZONE) {
      throw new IllegalArgumentException("timezone is too long");
    }
    try {
      ZoneId.of(zone);
    } catch (DateTimeException invalid) {
      throw new IllegalArgumentException("timezone is invalid: " + zone);
    }
    return zone;
  }
}
