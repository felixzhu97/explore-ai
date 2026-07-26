package com.ai.tools.infrastructure.config;

import com.ai.tools.domain.model.WeatherReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class ToolsConfig {

    @Bean
    WeatherReport weatherReport() {
        return new WeatherReport();
    }

    /**
     * System clock + JVM default zone so {@code LocaleContextHolder.getTimeZone()}
     * matches {@code app.datetime.zone} when no request locale is set (e.g. agents).
     */
    @Bean
    Clock systemClock(@Value("${app.datetime.zone:Asia/Shanghai}") String zoneId) {
        ZoneId zone = ZoneId.of(zoneId);
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        return Clock.system(zone);
    }
}
