package com.ai.tools.domain.vo;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;

public record WeatherQuery(String city, String normalizedCity) {

    public static WeatherQuery of(String city) {
        if (city == null || city.isBlank()) {
            throw new InvalidWeatherQueryException("City must not be blank");
        }
        return new WeatherQuery(city.trim(), city.trim().toLowerCase());
    }
}
