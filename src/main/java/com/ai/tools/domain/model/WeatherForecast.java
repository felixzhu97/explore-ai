package com.ai.tools.domain.model;

import com.ai.common.exception.DomainException;
import lombok.Value;

/** Forecast request for a city over one to seven days. */
@Value
public class WeatherForecast {
  WeatherQuery query;
  int days;

  public WeatherForecast(WeatherQuery query, int days) {
    if (query == null) {
      throw DomainException.createInvalidError("INVALID_WEATHER_QUERY", "Query must not be null");
    }
    if (days < 1 || days > 7) {
      throw DomainException.createInvalidError(
          "INVALID_WEATHER_QUERY", "Forecast days must be between 1 and 7");
    }
    this.query = query;
    this.days = days;
  }

  /** Creates a forecast request, three days by default. */
  public static WeatherForecast createForecast(WeatherQuery query, Integer days) {
    return new WeatherForecast(query, days != null ? days : 3);
  }
}
