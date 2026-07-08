package com.ai.tools.infrastructure.config;

import com.ai.tools.domain.model.WeatherReport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolsConfig {

    @Bean
    WeatherReport weatherReport() {
        return new WeatherReport();
    }
}
