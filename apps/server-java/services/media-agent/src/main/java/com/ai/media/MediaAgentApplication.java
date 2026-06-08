package com.ai.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MediaAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaAgentApplication.class, args);
    }
}
