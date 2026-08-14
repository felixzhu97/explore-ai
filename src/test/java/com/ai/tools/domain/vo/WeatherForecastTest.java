package com.ai.tools.domain.vo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeatherForecast")
class WeatherForecastTest {

  @Test
  @DisplayName("should reject null query")
  void shouldRejectNullQuery() {
    assertThatThrownBy(() -> WeatherForecast.of(null, 3))
        .isInstanceOf(InvalidWeatherQueryException.class);
  }

  @Test
  @DisplayName("should reject invalid days via compact constructor")
  void shouldRejectInvalidDaysViaCompactConstructor() {
    assertThatThrownBy(() -> new WeatherForecast(WeatherQuery.of("beijing"), 0))
        .isInstanceOf(InvalidWeatherQueryException.class);
  }
}
