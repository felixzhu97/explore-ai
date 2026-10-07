package com.ai.tools.domain.model;

import com.ai.common.exception.DomainException;

public record WeatherForecast(WeatherQuery query, int days) {
  public WeatherForecast {
    if (query == null) {
      throw DomainException.invalid("INVALID_WEATHER_QUERY", "Query must not be null");
    }
    if (days < 1 || days > 7) {
      throw DomainException.invalid(
          "INVALID_WEATHER_QUERY", "Forecast days must be between 1 and 7");
    }
  }

  /** Creates a forecast request, three days by default. */
  public static WeatherForecast of(WeatherQuery query, Integer days) {
    return new WeatherForecast(query, days != null ? days : 3);
  }
}
