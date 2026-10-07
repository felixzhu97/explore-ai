package com.ai.tools.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WeatherQuery")
class WeatherQueryTest {

  @Test
  @DisplayName("should normalize city name")
  void shouldNormalizeCityName() {
    WeatherQuery query = WeatherQuery.createQuery(" Beijing ");

    assertThat(query.normalizedCity()).isEqualTo("beijing");
  }

  @Test
  @DisplayName("should reject blank city")
  void shouldRejectBlankCity() {
    assertThatThrownBy(() -> WeatherQuery.createQuery(" "))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_WEATHER_QUERY");
  }

  @Test
  @DisplayName("should reject blank via compact constructor")
  void shouldRejectBlankViaCompactConstructor() {
    assertThatThrownBy(() -> new WeatherQuery(" ", " "))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "INVALID_WEATHER_QUERY");
  }
}
