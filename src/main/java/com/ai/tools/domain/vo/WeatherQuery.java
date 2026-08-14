package com.ai.tools.domain.vo;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;

/** Documentation. */
public record WeatherQuery(String city, String normalizedCity) {
  /** Documentation. */
  public WeatherQuery {
    if (city == null || city.isBlank()) {
      throw new InvalidWeatherQueryException("City must not be blank");
    }
    city = city.trim();
    normalizedCity = city.toLowerCase();
  }

  /** Documentation. */
  public static WeatherQuery of(String city) {
    return new WeatherQuery(city, city);
  }
}
