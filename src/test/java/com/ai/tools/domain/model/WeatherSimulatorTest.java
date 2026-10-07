package com.ai.tools.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeatherSimulator")
class WeatherSimulatorTest {

  private final WeatherSimulator weatherSimulator = new WeatherSimulator();

  @Test
  @DisplayName("should lookup known city weather")
  void shouldLookupKnownCityWeather() {
    var result = weatherSimulator.lookupCurrentWeather(WeatherQuery.createQuery("beijing"));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getContent()).contains("北京");
  }

  @Test
  @DisplayName("should generate forecast for unknown city")
  void shouldGenerateForecastForUnknownCity() {
    var result =
        weatherSimulator.generateForecast(
            WeatherForecast.createForecast(WeatherQuery.createQuery("unknown-city"), 3));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getContent()).contains("未来3天天气预报");
  }
}
