package com.ai.tools.infrastructure.tools;

import com.ai.tools.domain.exception.InvalidWeatherQueryException;
import com.ai.tools.domain.model.WeatherReport;
import com.ai.tools.domain.vo.WeatherForecast;
import com.ai.tools.domain.vo.WeatherQuery;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {

    private final WeatherReport weatherReport;

    public WeatherTools(WeatherReport weatherReport) {
        this.weatherReport = weatherReport;
    }

    @Tool(description = "获取指定城市的当前天气信息，包括温度、天气状况和湿度")
    public String getWeather(@ToolParam(description = "城市名称（中文或英文，如：北京、beijing）") String city) {
        try {
            return weatherReport.lookupCurrent(WeatherQuery.of(city)).content();
        } catch (InvalidWeatherQueryException e) {
            return e.getMessage();
        }
    }

    @Tool(description = "获取指定城市的天气预报，支持查询未来几天的天气")
    public String getForecast(
            @ToolParam(description = "城市名称（中文或英文）") String city,
            @ToolParam(description = "预报天数（1-7天）", required = false) Integer days) {
        try {
            WeatherQuery query = WeatherQuery.of(city);
            int forecastDays = days != null ? Math.max(1, Math.min(7, days)) : 3;
            return weatherReport.generateForecast(WeatherForecast.of(query, forecastDays)).content();
        } catch (InvalidWeatherQueryException e) {
            return e.getMessage();
        }
    }
}
