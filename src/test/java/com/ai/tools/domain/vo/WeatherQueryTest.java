package com.ai.tools.domain.vo;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WeatherQuery")
class WeatherQueryTest {

    @Test
    @DisplayName("should normalize city name")
    void should_normalize_city_name() {
        WeatherQuery query = WeatherQuery.of(" Beijing ");

        assertThat(query.normalizedCity()).isEqualTo("beijing");
    }

    @Test
    @DisplayName("should reject blank city")
    void should_reject_blank_city() {
        assertThatThrownBy(() -> WeatherQuery.of(" "))
                .isInstanceOf(InvalidWeatherQueryException.class);
    }

    @Test
    @DisplayName("should reject blank via compact constructor")
    void should_reject_blank_via_compact_constructor() {
        assertThatThrownBy(() -> new WeatherQuery(" ", " "))
                .isInstanceOf(InvalidWeatherQueryException.class);
    }
}
