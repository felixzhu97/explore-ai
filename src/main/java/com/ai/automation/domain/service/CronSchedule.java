package com.ai.automation.domain.service;

import java.time.Instant;

/** Computes cron fire times so the domain does not depend on a scheduling library. */
public interface CronSchedule {

  /**
   * Returns the first fire time after {@code after}, read in {@code timezone}.
   *
   * @throws IllegalArgumentException when the expression is invalid or never fires again
   */
  Instant nextRunAfter(String cronExpression, String timezone, Instant after);
}
