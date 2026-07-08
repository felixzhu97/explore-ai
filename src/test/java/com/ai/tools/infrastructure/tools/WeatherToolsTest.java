package com.ai.tools.infrastructure.tools;

import com.ai.tools.domain.model.WeatherReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherTools")
class WeatherToolsTest {

    private final WeatherTools weatherTools = new WeatherTools(new WeatherReport());

    @Nested
    @DisplayName("getWeather")
    class GetWeather {

        @ParameterizedTest
        @CsvSource({
                "beijing, 北京",
                "shanghai, 上海",
                "guangzhou, 广州",
                "shenzhen, 深圳",
                "chengdu, 成都",
                "hangzhou, 杭州"
        })
        @DisplayName("should return weather for known cities")
        void shouldReturnWeatherForKnownCities(String city, String expectedCityName) {
            String result = weatherTools.getWeather(city);

            assertThat(result).contains(expectedCityName);
            assertThat(result).contains("温度");
            assertThat(result).contains("天气");
            assertThat(result).contains("湿度");
        }

        @Test
        @DisplayName("should handle uppercase city name")
        void shouldHandleUppercaseCityName() {
            String result = weatherTools.getWeather("BEIJING");

            assertThat(result).contains("北京");
        }

        @Test
        @DisplayName("should handle city name with spaces")
        void shouldHandleCityNameWithSpaces() {
            String result = weatherTools.getWeather("  beijing  ");

            assertThat(result).contains("北京");
        }

        @ParameterizedTest
        @ValueSource(strings = {"tokyo", "paris", "london", "newyork"})
        @DisplayName("should return random weather for unknown cities")
        void shouldReturnRandomWeatherForUnknownCities(String city) {
            String result = weatherTools.getWeather(city);

            assertThat(result).contains(city);
            assertThat(result).contains("温度");
            assertThat(result).contains("天气");
            assertThat(result).contains("湿度");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return message for null or empty city")
        void shouldReturnMessageForNullOrEmptyCity(String city) {
            String result = weatherTools.getWeather(city);

            assertThat(result).isEqualTo("City must not be blank");
        }

        @Test
        @DisplayName("should return weather for blank city")
        void shouldReturnMessageForBlankCity() {
            String result = weatherTools.getWeather("   ");

            assertThat(result).isEqualTo("City must not be blank");
        }
    }

    @Nested
    @DisplayName("getForecast")
    class GetForecast {

        @Test
        @DisplayName("should return forecast for default days")
        void shouldReturnForecastForDefaultDays() {
            String result = weatherTools.getForecast("beijing", null);

            assertThat(result).contains("beijing");
            assertThat(result).contains("3");
            assertThat(result).contains("第1天");
            assertThat(result).contains("第2天");
            assertThat(result).contains("第3天");
        }

        @ParameterizedTest
        @CsvSource({
                "1, 1天",
                "3, 3天",
                "7, 7天"
        })
        @DisplayName("should return forecast for specified days")
        void shouldReturnForecastForSpecifiedDays(int days, String expected) {
            String result = weatherTools.getForecast("beijing", days);

            assertThat(result).contains(expected);
        }

        @Test
        @DisplayName("should cap forecast at 7 days when input is over 7")
        void shouldCapForecastAt7DaysWhenInputIsOver7() {
            String result = weatherTools.getForecast("beijing", 10);

            assertThat(result).contains("7");
        }

        @Test
        @DisplayName("should clamp to minimum one day when input is zero")
        void should_clamp_to_minimum_one_day_when_input_is_zero() {
            String result = weatherTools.getForecast("beijing", 0);

            assertThat(result).contains("1");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return message for null or empty city")
        void shouldReturnMessageForNullOrEmptyCity(String city) {
            String result = weatherTools.getForecast(city, 3);

            assertThat(result).isEqualTo("City must not be blank");
        }
    }
}
