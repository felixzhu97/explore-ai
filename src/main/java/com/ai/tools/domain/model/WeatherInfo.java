package com.ai.tools.domain.model;

public record WeatherInfo(String cityName, int temperature, String condition, int humidity) {

    public static WeatherInfo create(String cityName, int temperature, String condition, int humidity) {
        return new WeatherInfo(cityName, temperature, condition, humidity);
    }

    public String formatCurrent() {
        return String.format(
                "%s今天的天气：温度 %d°C，天气 %s，湿度 %d%%",
                cityName, temperature, condition, humidity);
    }
}
