package com.ai.tools.domain.vo;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;

public record WeatherForecast(WeatherQuery query, int days) {

    public static WeatherForecast of(WeatherQuery query, Integer days) {
        if (query == null) {
            throw new InvalidWeatherQueryException("Query must not be null");
        }
        int forecastDays = days != null ? days : 3;
        if (forecastDays < 1 || forecastDays > 7) {
            throw new InvalidWeatherQueryException("Forecast days must be between 1 and 7");
        }
        return new WeatherForecast(query, forecastDays);
    }
}
