package com.ai.billing.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import lombok.Value;

/** Requests counted for one quota subject on one UTC day; a new day starts from zero. */
@Value
public class DailyUsage {
  LocalDate day;
  int count;

  public DailyUsage(LocalDate day, int count) {
    Objects.requireNonNull(day, "day");
    if (count < 0) {
      throw new IllegalArgumentException("count must not be negative");
    }
    this.day = day;
    this.count = count;
  }

  /** Returns empty usage for the day. */
  public static DailyUsage createEmptyUsage(LocalDate day) {
    return new DailyUsage(day, 0);
  }

  /** Returns the requests left under the limit today. */
  public int calculateRemaining(int limit, LocalDate today) {
    return Math.max(0, limit - countOn(today));
  }

  /** Returns usage with one more request today, starting over when the day changed. */
  public DailyUsage consumeQuota(LocalDate today) {
    return new DailyUsage(today, countOn(today) + 1);
  }

  /** Gives back a request consumed today, such as when another limit refused the call. */
  public DailyUsage release(LocalDate today) {
    return new DailyUsage(today, Math.max(0, countOn(today) - 1));
  }

  private int countOn(LocalDate today) {
    return day.equals(today) ? count : 0;
  }
}
