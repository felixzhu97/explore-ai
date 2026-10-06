package com.ai.chat.controller.dto;

import com.ai.common.controller.dto.HealthStatus;

/** Health check response DTO. */
public record HealthResponse(HealthStatus status) {
  /** Creates a healthy response. */
  public static HealthResponse up() {
    return new HealthResponse(HealthStatus.UP);
  }
}
