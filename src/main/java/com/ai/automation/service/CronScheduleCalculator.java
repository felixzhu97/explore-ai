package com.ai.automation.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/** Documentation. */
@Component
public class CronScheduleCalculator {
  /** Documentation. */
  public Instant nextRunAt(String cronExpression, String timezone, Instant after) {
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

  /** Documentation. */
  public void validate(String cronExpression, String timezone) {
    CronExpression.parse(cronExpression);
    ZoneId.of(timezone);
    nextRunAt(cronExpression, timezone, Instant.now());
  }
}
