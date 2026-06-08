package com.ai.vision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.ai.vision.config.VisionProperties;

@SpringBootApplication
@EnableConfigurationProperties(VisionProperties.class)
public class VisionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VisionServiceApplication.class, args);
	}
}
