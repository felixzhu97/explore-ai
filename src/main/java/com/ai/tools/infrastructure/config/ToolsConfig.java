package com.ai.tools.infrastructure.config;

import com.ai.tools.domain.model.WeatherReport;
import com.ai.tools.domain.service.CurrentDateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ToolsConfig {

    @Bean
    WeatherReport weatherReport() {
        return new WeatherReport();
    }

    @Bean
    Clock systemClock(@Value("${app.datetime.zone:Asia/Shanghai}") String zoneId) {
        return Clock.system(ZoneId.of(zoneId));
    }

    @Bean
    CurrentDateTimeFormatter currentDateTimeFormatter(
            Clock clock,
            @Value("${app.datetime.zone:Asia/Shanghai}") String zoneId) {
        return new CurrentDateTimeFormatter(clock, ZoneId.of(zoneId));
    }
}
