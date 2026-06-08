package com.ai.vision.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.nio.file.Path;

@Configuration
@EnableWebFlux
public class WebFluxConfig {

	@Bean
	public Path visionTempDir() {
		try {
			Path tempDir = java.nio.file.Files.createTempDirectory("vision-");
			return tempDir;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create vision temp directory", e);
		}
	}
}
