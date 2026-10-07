package com.ai.tools.infra.tools;

import com.ai.common.exception.DomainException;
import com.ai.tools.domain.model.WeatherForecast;
import com.ai.tools.domain.model.WeatherQuery;
import com.ai.tools.domain.model.WeatherReport;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Spring AI tool callbacks exposing current weather and multi-day forecasts to the model. */
@Component
@RequiredArgsConstructor
public class WeatherTools {

  private final WeatherReport weatherReport;

  @Tool(
      description =
          """
            Current weather for a city (当前天气 / weather now). Prefer this over
            searchWeb for temperature, conditions, and humidity. Do not invent readings.""")
  public String getWeather(
      @ToolParam(description = "City name in Chinese or English (e.g. 北京, beijing)") String city) {
    try {
      return weatherReport.lookupCurrent(WeatherQuery.of(city)).content();
    } catch (DomainException e) {
      return e.getMessage();
    }
  }

  @Tool(
      description =
          """
            Multi-day forecast for a city (天气预报 / forecast). Prefer this over
            searchWeb for upcoming weather. Days default to 3 when omitted (1–7).""")
  public String getForecast(
      @ToolParam(description = "City name in Chinese or English") String city,
      @ToolParam(description = "Forecast days (1-7)", required = false) Integer days) {
    try {
      WeatherQuery query = WeatherQuery.of(city);
      int forecastDays = days != null ? Math.max(1, Math.min(7, days)) : 3;
      return weatherReport.generateForecast(WeatherForecast.of(query, forecastDays)).content();
    } catch (DomainException e) {
      return e.getMessage();
    }
  }
}
