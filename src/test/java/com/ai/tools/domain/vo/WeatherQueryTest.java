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
    void shouldNormalizeCityName() {
        WeatherQuery query = WeatherQuery.of(" Beijing ");

        assertThat(query.normalizedCity()).isEqualTo("beijing");
    }

    @Test
    @DisplayName("should reject blank city")
    void shouldRejectBlankCity() {
        assertThatThrownBy(() -> WeatherQuery.of(" "))
                .isInstanceOf(InvalidWeatherQueryException.class);
    }

    @Test
    @DisplayName("should reject blank via compact constructor")
    void shouldRejectBlankViaCompactConstructor() {
        assertThatThrownBy(() -> new WeatherQuery(" ", " "))
                .isInstanceOf(InvalidWeatherQueryException.class);
    }
}
