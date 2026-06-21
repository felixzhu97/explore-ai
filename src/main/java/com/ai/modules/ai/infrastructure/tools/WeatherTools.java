package com.ai.modules.ai.infrastructure.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Weather tools for AI function calling.
 * Demonstrates Spring AI 2.0 Tool Calling with @Tool annotation.
 */
@Component
public class WeatherTools {

    private static final Map<String, WeatherInfo> WEATHER_DATA = Map.of(
            "beijing", new WeatherInfo("北京", 25, "晴", 65),
            "shanghai", new WeatherInfo("上海", 28, "多云", 72),
            "guangzhou", new WeatherInfo("广州", 32, "晴", 80),
            "shenzhen", new WeatherInfo("深圳", 31, "阴", 78),
            "chengdu", new WeatherInfo("成都", 24, "小雨", 85),
            "hangzhou", new WeatherInfo("杭州", 27, "晴", 68),
            "wuhan", new WeatherInfo("武汉", 29, "多云", 70),
            "xian", new WeatherInfo("西安", 26, "晴", 55),
            "nanjing", new WeatherInfo("南京", 28, "晴", 62),
            "tianjin", new WeatherInfo("天津", 26, "多云", 58)
    );

    @Tool(description = "获取指定城市的当前天气信息，包括温度、天气状况和湿度")
    public String getWeather(@ToolParam(description = "城市名称（中文或英文，如：北京、beijing）") String city) {
        if (city == null || city.isBlank()) {
            return "请提供有效的城市名称";
        }

        String normalizedCity = city.toLowerCase().trim();

        WeatherInfo weather = WEATHER_DATA.get(normalizedCity);
        if (weather != null) {
            return String.format(
                    "%s今天的天气：温度 %d°C，天气 %s，湿度 %d%%",
                    weather.cityName(), weather.temperature(), weather.condition(), weather.humidity()
            );
        }

        // 模拟其他城市的随机天气
        int temp = 20 + ThreadLocalRandom.current().nextInt(15);
        String[] conditions = {"晴", "多云", "阴", "小雨", "晴转多云"};
        String condition = conditions[ThreadLocalRandom.current().nextInt(conditions.length)];
        int humidity = 50 + ThreadLocalRandom.current().nextInt(40);

        return String.format(
                "%s今天的天气：温度 %d°C，天气 %s，湿度 %d%%",
                city, temp, condition, humidity
        );
    }

    @Tool(description = "获取指定城市的天气预报，支持查询未来几天的天气")
    public String getForecast(
            @ToolParam(description = "城市名称（中文或英文）") String city,
            @ToolParam(description = "预报天数（1-7天）", required = false) Integer days
    ) {
        if (city == null || city.isBlank()) {
            return "请提供有效的城市名称";
        }

        int forecastDays = (days != null && days >= 1 && days <= 7) ? days : 3;

        StringBuilder forecast = new StringBuilder();
        forecast.append(city).append("未来").append(forecastDays).append("天天气预报：\n");

        String[] conditions = {"晴", "多云", "阴", "小雨", "晴转多云"};
        int[] temps = {18, 20, 22, 25, 28, 30, 32};

        for (int i = 1; i <= forecastDays; i++) {
            String condition = conditions[ThreadLocalRandom.current().nextInt(conditions.length)];
            int tempLow = temps[ThreadLocalRandom.current().nextInt(temps.length)];
            int tempHigh = tempLow + 3 + ThreadLocalRandom.current().nextInt(5);
            forecast.append(String.format("第%d天：%s，最高%d°C，最低%d°C\n",
                    i, condition, tempHigh, tempLow));
        }

        return forecast.toString();
    }

    private record WeatherInfo(String cityName, int temperature, String condition, int humidity) {
    }
}
