package com.ai.tools.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherInfo")
class WeatherInfoTest {

    @Test
    @DisplayName("should format current weather report")
    void should_format_current_weather_report() {
        WeatherInfo info = new WeatherInfo("北京", 25, "晴", 65);

        assertThat(info.formatCurrent()).contains("北京").contains("25°C").contains("晴");
    }
}
