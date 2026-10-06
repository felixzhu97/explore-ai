package com.ai.common.controller.dto;

/** Health of a module or provider as reported by health endpoints. */
public enum HealthStatus {
  UP,
  DOWN,
  DEGRADED;

  /** Maps a health flag to a status. */
  public static HealthStatus of(boolean healthy) {
    return healthy ? UP : DOWN;
  }
}
