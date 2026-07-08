package com.ai.tools.domain.vo;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;

public record WeatherQuery(String city, String normalizedCity) {

    public WeatherQuery {
        if (city == null || city.isBlank()) {
            throw new InvalidWeatherQueryException("City must not be blank");
        }
        city = city.trim();
        normalizedCity = city.toLowerCase();
    }

    public static WeatherQuery of(String city) {
        return new WeatherQuery(city, city);
    }
}
