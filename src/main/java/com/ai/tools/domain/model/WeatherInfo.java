package com.ai.tools.domain.model;

import lombok.Value;

/** Current weather of a city. */
@Value
public class WeatherInfo {
  String cityName;
  int temperature;
  String condition;
  int humidity;

  /** Formats today's weather as text. */
  public String formatCurrentWeather() {
    return String.format(
        "%s今天的天气：温度 %d°C，天气 %s，湿度 %d%%", cityName, temperature, condition, humidity);
  }
}
