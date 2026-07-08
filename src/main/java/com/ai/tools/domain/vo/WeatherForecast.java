package com.ai.tools.domain.vo;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;

public record WeatherForecast(WeatherQuery query, int days) {

    public WeatherForecast {
        if (query == null) {
            throw new InvalidWeatherQueryException("Query must not be null");
        }
        if (days < 1 || days > 7) {
            throw new InvalidWeatherQueryException("Forecast days must be between 1 and 7");
        }
    }

    public static WeatherForecast of(WeatherQuery query, Integer days) {
        return new WeatherForecast(query, days != null ? days : 3);
    }
}
