package com.ai.tools.domain.model;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Produces simulated weather reports from built-in city data, falling back to random values. */
public class WeatherSimulator {

  private static final Map<String, WeatherInfo> WEATHER_DATA =
      Map.of(
          "beijing", new WeatherInfo("北京", 25, "晴", 65),
          "shanghai", new WeatherInfo("上海", 28, "多云", 72),
          "guangzhou", new WeatherInfo("广州", 32, "晴", 80),
          "shenzhen", new WeatherInfo("深圳", 31, "阴", 78),
          "chengdu", new WeatherInfo("成都", 24, "小雨", 85),
          "hangzhou", new WeatherInfo("杭州", 27, "晴", 68),
          "wuhan", new WeatherInfo("武汉", 29, "多云", 70),
          "xian", new WeatherInfo("西安", 26, "晴", 55),
          "nanjing", new WeatherInfo("南京", 28, "晴", 62),
          "tianjin", new WeatherInfo("天津", 26, "多云", 58));

  private static final String[] CONDITIONS = {"晴", "多云", "阴", "小雨", "晴转多云"};
  private static final int[] TEMPS = {18, 20, 22, 25, 28, 30, 32};

  /** Returns current weather for a known city, or randomly generated readings for others. */
  public ToolResult lookupCurrentWeather(WeatherQuery query) {
    WeatherInfo known = WEATHER_DATA.get(query.getNormalizedCity());
    if (known != null) {
      return ToolResult.createSuccessResult(known.formatCurrentWeather());
    }
    return ToolResult.createSuccessResult(buildRandomCurrent(query.getCity()));
  }

  /** Builds a day-by-day forecast with randomly chosen conditions and temperature ranges. */
  public ToolResult generateForecast(WeatherForecast forecast) {
    StringBuilder builder = new StringBuilder();
    builder
        .append(forecast.getQuery().getCity())
        .append("未来")
        .append(forecast.getDays())
        .append("天天气预报：\n");

    for (int day = 1; day <= forecast.getDays(); day++) {
      String condition = CONDITIONS[ThreadLocalRandom.current().nextInt(CONDITIONS.length)];
      int tempLow = TEMPS[ThreadLocalRandom.current().nextInt(TEMPS.length)];
      int tempHigh = tempLow + 3 + ThreadLocalRandom.current().nextInt(5);
      builder.append(formatForecastDay(day, condition, tempHigh, tempLow)).append('\n');
    }

    return ToolResult.createSuccessResult(builder.toString().trim());
  }

  private static String formatForecastDay(int day, String condition, int tempHigh, int tempLow) {
    return String.format("第%d天：%s，最高%d°C，最低%d°C", day, condition, tempHigh, tempLow);
  }

  private String buildRandomCurrent(String city) {
    int temp = 20 + ThreadLocalRandom.current().nextInt(15);
    String condition = CONDITIONS[ThreadLocalRandom.current().nextInt(CONDITIONS.length)];
    int humidity = 50 + ThreadLocalRandom.current().nextInt(40);
    return new WeatherInfo(city, temp, condition, humidity).formatCurrentWeather();
  }
}
