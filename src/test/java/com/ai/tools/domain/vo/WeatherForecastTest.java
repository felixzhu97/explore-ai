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
}
