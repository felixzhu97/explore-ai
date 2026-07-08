package com.ai.tools.domain.model;

import com.ai.tools.domain.vo.WeatherForecast;
import com.ai.tools.domain.vo.WeatherQuery;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WeatherReport {

    private static final Map<String, WeatherInfo> WEATHER_DATA = Map.of(
            "beijing", WeatherInfo.create("北京", 25, "晴", 65),
            "shanghai", WeatherInfo.create("上海", 28, "多云", 72),
            "guangzhou", WeatherInfo.create("广州", 32, "晴", 80),
            "shenzhen", WeatherInfo.create("深圳", 31, "阴", 78),
            "chengdu", WeatherInfo.create("成都", 24, "小雨", 85),
            "hangzhou", WeatherInfo.create("杭州", 27, "晴", 68),
            "wuhan", WeatherInfo.create("武汉", 29, "多云", 70),
            "xian", WeatherInfo.create("西安", 26, "晴", 55),
            "nanjing", WeatherInfo.create("南京", 28, "晴", 62),
            "tianjin", WeatherInfo.create("天津", 26, "多云", 58));

    private static final String[] CONDITIONS = {"晴", "多云", "阴", "小雨", "晴转多云"};
    private static final int[] TEMPS = {18, 20, 22, 25, 28, 30, 32};

    public ToolResult lookupCurrent(WeatherQuery query) {
        WeatherInfo known = WEATHER_DATA.get(query.normalizedCity());
        if (known != null) {
            return ToolResult.success(known.formatCurrent());
        }
        return ToolResult.success(buildRandomCurrent(query.city()));
    }

    public ToolResult generateForecast(WeatherForecast forecast) {
        StringBuilder builder = new StringBuilder();
        builder.append(forecast.query().city())
                .append("未来")
                .append(forecast.days())
                .append("天天气预报：\n");

        for (int day = 1; day <= forecast.days(); day++) {
            String condition = CONDITIONS[ThreadLocalRandom.current().nextInt(CONDITIONS.length)];
            int tempLow = TEMPS[ThreadLocalRandom.current().nextInt(TEMPS.length)];
            int tempHigh = tempLow + 3 + ThreadLocalRandom.current().nextInt(5);
            builder.append(formatForecastDay(day, condition, tempHigh, tempLow)).append('\n');
        }

        return ToolResult.success(builder.toString().trim());
    }

    private static String formatForecastDay(int day, String condition, int tempHigh, int tempLow) {
        return String.format("第%d天：%s，最高%d°C，最低%d°C", day, condition, tempHigh, tempLow);
    }

    private String buildRandomCurrent(String city) {
        int temp = 20 + ThreadLocalRandom.current().nextInt(15);
        String condition = CONDITIONS[ThreadLocalRandom.current().nextInt(CONDITIONS.length)];
        int humidity = 50 + ThreadLocalRandom.current().nextInt(40);
        return WeatherInfo.create(city, temp, condition, humidity).formatCurrent();
    }
}
