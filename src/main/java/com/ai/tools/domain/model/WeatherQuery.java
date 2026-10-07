package com.ai.tools.domain.model;

import com.ai.common.exception.DomainException;

public record WeatherQuery(String city, String normalizedCity) {
  public WeatherQuery {
    if (city == null || city.isBlank()) {
      throw DomainException.invalid("INVALID_WEATHER_QUERY", "City must not be blank");
    }
    city = city.trim();
    normalizedCity = city.toLowerCase();
  }

  /** Creates a weather query for the city. */
  public static WeatherQuery createQuery(String city) {
    return new WeatherQuery(city, city);
  }
}
