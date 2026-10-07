package com.ai.tools.domain.model;

import com.ai.common.exception.DomainException;
import lombok.Value;

/** City to look up, as typed and normalized. */
@Value
public class WeatherQuery {
  String city;
  String normalizedCity;

  public WeatherQuery(String city, String normalizedCity) {
    if (city == null || city.isBlank()) {
      throw DomainException.invalid("INVALID_WEATHER_QUERY", "City must not be blank");
    }
    city = city.trim();
    normalizedCity = city.toLowerCase();
    this.city = city;
    this.normalizedCity = normalizedCity;
  }

  /** Creates a weather query for the city. */
  public static WeatherQuery createQuery(String city) {
    return new WeatherQuery(city, city);
  }
}
