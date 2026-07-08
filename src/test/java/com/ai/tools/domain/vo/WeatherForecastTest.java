package com.ai.tools.domain.vo;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WeatherForecast")
class WeatherForecastTest {

    @Test
    @DisplayName("should reject null query")
    void should_reject_null_query() {
        assertThatThrownBy(() -> WeatherForecast.of(null, 3))
                .isInstanceOf(InvalidWeatherQueryException.class);
    }

    @Test
    @DisplayName("should reject invalid days via compact constructor")
    void should_reject_invalid_days_via_compact_constructor() {
        assertThatThrownBy(() -> new WeatherForecast(WeatherQuery.of("beijing"), 0))
                .isInstanceOf(InvalidWeatherQueryException.class);
    }
}
