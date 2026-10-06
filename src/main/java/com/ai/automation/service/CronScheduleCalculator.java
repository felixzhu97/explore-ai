package com.ai.automation.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/** Computes and validates cron fire times in a schedule's time zone. */
@Component
public class CronScheduleCalculator {
  /** Returns the next cron fire time after {@code after}, evaluated in the given time zone. */
  public Instant calculateNextRunAt(String cronExpression, String timezone, Instant after) {
    CronExpression cron = CronExpression.parse(cronExpression);
    ZoneId zone = ZoneId.of(timezone);
    ZonedDateTime base = after.atZone(zone);
    ZonedDateTime next = cron.next(base);
    if (next == null) {
      throw new IllegalArgumentException(
          "Cron expression has no next fire time: " + cronExpression);
    }
    return next.toInstant();
  }

  /** Checks that the cron expression and time zone are valid. */
  public void validate(String cronExpression, String timezone) {
    CronExpression.parse(cronExpression);
    ZoneId.of(timezone);
    calculateNextRunAt(cronExpression, timezone, Instant.now());
  }
}
