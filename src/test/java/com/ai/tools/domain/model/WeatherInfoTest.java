package com.ai.tools.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeatherInfo")
class WeatherInfoTest {

  @Test
  @DisplayName("should format current weather report")
  void shouldFormatCurrentWeatherReport() {
    WeatherInfo info = new WeatherInfo("北京", 25, "晴", 65);

    assertThat(info.formatCurrent()).contains("北京").contains("25°C").contains("晴");
  }
}
