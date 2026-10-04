package com.ai.chat.controller.dto;

/** Health check response DTO. */
public record HealthResponse(String status) {
  public static HealthResponse up() {
    return new HealthResponse("UP");
  }
}
