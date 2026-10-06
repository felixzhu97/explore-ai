package com.ai.automation.infra.schedule;

import com.ai.automation.domain.service.CronSchedule;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/** Computes cron fire times with Spring's six-field cron syntax. */
@Component
public class SpringCronSchedule implements CronSchedule {

  @Override
  public Instant nextRunAfter(String cronExpression, String timezone, Instant after) {
    CronExpression cron = CronExpression.parse(cronExpression);
    ZonedDateTime next = cron.next(after.atZone(ZoneId.of(timezone)));
    if (next == null) {
      throw new IllegalArgumentException(
          "Cron expression has no next fire time: " + cronExpression);
    }
    return next.toInstant();
  }
}
