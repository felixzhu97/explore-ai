package com.ai.tools.domain.model;

public class WeatherInfo {

    private final String cityName;
    private final int temperature;
    private final String condition;
    private final int humidity;

    private WeatherInfo(String cityName, int temperature, String condition, int humidity) {
        this.cityName = cityName;
        this.temperature = temperature;
        this.condition = condition;
        this.humidity = humidity;
    }

    public static WeatherInfo create(String cityName, int temperature, String condition, int humidity) {
        return new WeatherInfo(cityName, temperature, condition, humidity);
    }

    public String formatCurrent() {
        return String.format(
                "%s今天的天气：温度 %d°C，天气 %s，湿度 %d%%",
                cityName, temperature, condition, humidity);
    }

    public String formatForecastDay(int day, int tempHigh, int tempLow, String dayCondition) {
        return String.format("第%d天：%s，最高%d°C，最低%d°C", day, dayCondition, tempHigh, tempLow);
    }

    public String cityName() {
        return cityName;
    }

    public int temperature() {
        return temperature;
    }

    public String condition() {
        return condition;
    }

    public int humidity() {
        return humidity;
    }
}
