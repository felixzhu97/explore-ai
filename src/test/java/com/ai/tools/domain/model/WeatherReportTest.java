package com.ai.tools.domain.model;

import com.ai.tools.domain.vo.WeatherForecast;
import com.ai.tools.domain.vo.WeatherQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherReport")
class WeatherReportTest {

    private final WeatherReport weatherReport = new WeatherReport();

    @Test
    @DisplayName("should lookup known city weather")
    void should_lookup_known_city_weather() {
        var result = weatherReport.lookupCurrent(WeatherQuery.of("beijing"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.content()).contains("北京");
    }

    @Test
    @DisplayName("should generate forecast for unknown city")
    void should_generate_forecast_for_unknown_city() {
        var result = weatherReport.generateForecast(
                WeatherForecast.of(WeatherQuery.of("unknown-city"), 3));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.content()).contains("未来3天天气预报");
    }
}
